/*
 *  Copyright (c) 2022-2022 ForteScarlet <ForteScarlet@163.com>
 *
 *  本文件是 simply-robot (或称 simple-robot 3.x 、simbot 3.x ) 的一部分。
 *
 *  simply-robot 是自由软件：你可以再分发之和/或依照由自由软件基金会发布的 GNU 通用公共许可证修改之，无论是版本 3 许可证，还是（按你的决定）任何以后版都可以。
 *
 *  发布 simply-robot 是希望它能有用，但是并无保障;甚至连可销售和符合某个特定的目的都不保证。请参看 GNU 通用公共许可证，了解详情。
 *
 *  你应该随程序获得一份 GNU 通用公共许可证的复本。如果没有，请看:
 *  https://www.gnu.org/licenses
 *  https://www.gnu.org/licenses/gpl-3.0-standalone.html
 *  https://www.gnu.org/licenses/lgpl-3.0-standalone.html
 *
 */

package love.forte.simboot.core.listener

import love.forte.annotationtool.core.KAnnotationTool
import love.forte.annotationtool.core.getAnnotation
import love.forte.di.BeanContainer
import love.forte.simboot.annotation.*
import love.forte.simboot.annotation.Filter
import love.forte.simboot.core.filter.CoreFiltersAnnotationProcessor
import love.forte.simboot.core.filter.FiltersAnnotationProcessContext
import love.forte.simboot.interceptor.AnnotatedEventListenerInterceptor
import love.forte.simboot.listener.BindException
import love.forte.simboot.listener.ParameterBinder
import love.forte.simboot.listener.ParameterBinderFactory
import love.forte.simboot.listener.ParameterBinderResult
import love.forte.simbot.*
import love.forte.simbot.core.event.plus
import love.forte.simbot.event.*
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Named
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.functions
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.isSubclassOf
import love.forte.simboot.annotation.Interceptor as InterceptorAnnotation


/**
 *
 * 解析一个 [KFunction] 并将其注册为一个监听函数。
 *
 * @author ForteScarlet
 */
public class KFunctionListenerProcessor {
    private val instanceCache = ConcurrentHashMap<KClass<*>, Any>()
    private val annotationTool = KAnnotationTool()
    private val logger = LoggerFactory.getLogger(KFunctionListenerProcessor::class)
    
    
    /**
     * 提供参数并将其解析为监听函数。
     *
     *
     */
    public fun process(context: FunctionalListenerProcessContext): EventListener {
        val function = context.function.also { it.checkLegal() }
        
        val functionId = context.id ?: function.sign()
        val functionLogger = LoggerFactory.getLogger("love.forte.simbot.listener.${function.name}")
        val listenTargets = function.listenTargets()
        val binders = function.binders(context)
        val listenerAttributeMap = AttributeMutableMap(ConcurrentHashMap())
        
        // attributes
        listenerAttributeMap[RAW_FUNCTION] = function
        listenerAttributeMap[RAW_BINDERS] = binders
        listenerAttributeMap[RAW_LISTEN_TARGETS] = listenTargets
        
        val listener = KFunctionEventListener(
            id = functionId.ID,
            priority = context.priority,
            isAsync = context.isAsync,
            targets = listenTargets.toSet(),
            caller = function,
            logger = functionLogger,
            binders = binders.toTypedArray(),
            attributeMap = listenerAttributeMap,
        )
        
        // filters
        val filters = function.filters(listener, listenerAttributeMap, context)
        logger.debug("Size of resolved listener filters: {}", filters.size)
        logger.debug("Resolved listener filters: {}", filters)
        
        // interceptors
        val interceptors = function.interceptors(context)
        logger.debug("Size of resolved listener interceptors: {}", interceptors.size)
        logger.debug("Resolved listener interceptors: {}", interceptors)
        
        
        var resolvedListener: EventListener = listener
        
        if (filters.isNotEmpty()) {
            resolvedListener += filters
        }
        
        if (interceptors.isNotEmpty()) {
            resolvedListener += interceptors
        }
        
        return resolvedListener
    }
    
    
    /**
     * 函数的访问级别必须是public的, 且不能是抽象的。
     */
    private fun KFunction<*>.checkLegal() {
        val isPublic = kotlin.runCatching { visibility == KVisibility.PUBLIC }.getOrElse { false }
        if (!isPublic) {
            throw SimbotIllegalStateException("The visibility of listener function [$this] must be [PUBLIC], but $visibility")
        }
        
        if (isAbstract) {
            throw SimbotIllegalStateException("The listener function [$this] must not be abstract, but it is.")
        }
    }
    
    /**
     * 构建一个此监听函数的签名。一般类似于全限定名。
     */
    private fun KFunction<*>.sign(): String {
        return buildString {
            instanceParameter?.type?.also {
                append(it.toString())
                append('.')
            }
            append(name)
            val pms = parameters.filter { it.kind != KParameter.Kind.INSTANCE }
            if (pms.isNotEmpty()) {
                append('(')
                pms.joinTo(this, separator = ",") {
                    it.type.toString()
                }
                append(')')
            }
        }
    }
    
    /**
     * 解析此监听函数所期望监听的事件列表。如果有 [Listens] 注解, 取其值, 否则在参数中寻找。
     */
    private fun KFunction<*>.listenTargets(): Set<Event.Key<*>> {
        val listenList = annotationTool.getAnnotations(this, Listen::class)
        val targetSet = mutableSetOf<Event.Key<*>>()
        val targets = if (listenList.isNotEmpty()) {
            // 使用它们
            logger.debug("Annotation @Listen on listener function [{}] is not empty, use them: {}", this, listenList)
            listenList.mapTo(targetSet) {
                val e = it.value
                try {
                    e.getKey()
                } catch (e: NoSuchEventKeyDefineException) {
                    throw SimbotIllegalStateException("Event type [$e] cannot be listen: there is no Event.Key<?> for it")
                }
            }
        } else {
            // find in parameters
            parameters.asSequence().filter { it.kind != KParameter.Kind.INSTANCE }
                .filter { (it.type.classifier as? KClass<*>)?.isSubclassOf(Event::class) == true }.mapTo(targetSet) {
                    val e = it.type.classifier
                    @Suppress("UNCHECKED_CAST") try {
                        (e as KClass<out Event>).getKey()
                    } catch (e: NoSuchEventKeyDefineException) {
                        throw SimbotIllegalStateException("Event type parameter [$it] cannot be listen: there is no Event.Key<?> for it")
                    }
                }
        }
        
        
        return targets
    }
    
    /**
     * 为当前函数匹配并提供参数绑定器。
     */
    private fun KFunction<*>.binders(context: FunctionalListenerProcessContext): List<ParameterBinder> {
        val binderFactoryContainer = context.binderManager
        val binderFactories = binderFactoryContainer.getGlobals().toMutableList()
        val currentBinderAnnotation = annotationTool.getAnnotation(this, Binder::class)
        if (currentBinderAnnotation != null) {
            if (currentBinderAnnotation.scopeIfDefault { Binder.Scope.SPECIFY } != Binder.Scope.SPECIFY) {
                throw SimbotIllegalStateException("Listener function [$this] annotate @Binder, But the scope is not Scope.SPECIFY.")
            }
            
            currentBinderAnnotation.value.ifEmpty {
                // warn.
                logger.warn("Listener function [$this] annotate @Binder, But the [value] is empty.")
                emptyArray()
            }.forEach {
                val binderFactoryInContainer = binderFactoryContainer[it]
                    ?: throw SimbotIllegalStateException("Cannot found binder factory by id [$it] annotate on listener function [$this] in container $binderFactoryContainer")
                
                logger.debug("load specify binder factory {} for listener function {}", binderFactoryInContainer, this)
                binderFactories.add(binderFactoryInContainer)
            }
        }
        
        // current binders
        val currentClass = kotlin.runCatching { instanceParameter?.type?.classifier as? KClass<*> }.getOrElse {
            logger.warn("Listener function $this has no instance parameter.")
            null
        }
        
        if (currentClass != null) {
            kotlin.runCatching {
                currentClass.functions
                    .forEach {
                        // to binder factory
                        val binder =
                            kotlin.runCatching { annotationTool.getAnnotation(it, Binder::class) }.getOrElse { e ->
                                logger.debug("Cannot get annotation @Binder from function $it", e)
                                null
                            } ?: return@forEach
                        
                        if (binder.scopeIfDefault { Binder.Scope.CURRENT } != Binder.Scope.CURRENT) {
                            logger.warn("The function [{}] annotated @Binder, but the scope is not CURRENT.", it)
                            return@forEach
                        }
                        
                        kotlin.runCatching {
                            val objInstance = currentClass.objectInstance
                            val factory = if (objInstance != null) {
                                binderFactoryContainer.resolveFunctionToBinderFactory(function = it) { objInstance }
                            } else {
                                val name = annotationTool.getAnnotation<Named>(currentClass)?.value
                                if (name != null) {
                                    binderFactoryContainer.resolveFunctionToBinderFactory(function = it) { context ->
                                        context.beanContainer[name, currentClass]
                                    }
                                } else {
                                    binderFactoryContainer.resolveFunctionToBinderFactory(function = it) { context ->
                                        context.beanContainer[currentClass]
                                    }
                                }
                                
                            }
                            
                            binderFactories.add(factory)
                        }.getOrElse { e ->
                            logger.debug("Resolve function $it to binder factory failure: ${e.localizedMessage}", e)
                        }
                    }
            }
        }
        
        
        
        return binderFactoriesToBinder(logger, context, binderFactories)
    }
    
    /**
     * 将binder工厂集合转化为binder集合。
     */
    private fun KFunction<*>.binderFactoriesToBinder(
        logger: Logger,
        context: FunctionalListenerProcessContext,
        factories: List<ParameterBinderFactory>,
    ): List<ParameterBinder> {
        val bindFactories = factories.sortedBy { it.priority }
        val binders = parameters.map { parameter ->
            val bindContext = ParameterBinderFactoryContextImpl(
                context.beanContainer,
                context.function,
                parameter
            )
            
            val bindList = mutableListOf<ParameterBinderResult.NotEmpty>()
            val bindSpareList = mutableListOf<ParameterBinderResult.NotEmpty>()
            
            for (factory in bindFactories) {
                when (val result = factory.resolveToBinder(bindContext)) {
                    is ParameterBinderResult.Empty -> continue
                    is ParameterBinderResult.NotEmpty -> {
                        // not empty.
                        when (result) {
                            is ParameterBinderResult.Normal -> {
                                if (bindList.isEmpty() || (bindList.first() !is ParameterBinderResult.Only)) {
                                    bindList.add(result)
                                }
                            }
                            is ParameterBinderResult.Only -> {
                                if (bindList.isNotEmpty() && bindList.first() is ParameterBinderResult.Only) {
                                    // 上一个也是Only.
                                    bindList[0] = result
                                } else {
                                    bindList.clear() // clear all
                                    bindList.add(result)
                                }
                            }
                            is ParameterBinderResult.Spare -> {
                                bindSpareList.add(result)
                            }
                        }
                    }
                }
            }
            
            bindList.sortBy { it.priority }
            bindSpareList.sortBy { it.priority }
            
            logger.debug("There are actually {} normal binders bound to parameter [{}]. the binders: {}",
                bindList.size,
                parameter,
                bindList)
            logger.debug("There are actually {} spare binders bound to parameter [{}]. the binders: {}",
                bindSpareList.size,
                parameter,
                bindSpareList)
            
            when {
                bindList.isEmpty() && bindSpareList.isEmpty() -> {
                    // no binder.
                    EmptyBinder(parameter)
                }
                bindList.isEmpty() -> {
                    // spare as normal.
                    MergedBinder(bindSpareList.map { it.binder }, emptyList(), parameter)
                }
                else -> {
                    MergedBinder(
                        bindList.map { it.binder },
                        bindSpareList.map { it.binder }.ifEmpty { emptyList() },
                        parameter
                    )
                }
            }
        }
        
        return binders
    }
    
    /**
     * 解析获取函数上的标准过滤器注解。
     */
    private fun KFunction<*>.filters(
        listener: EventListener,
        listenerAttributeMap: MutableAttributeMap,
        context: FunctionalListenerProcessContext,
    ): List<EventFilter> {
        val filters = annotationTool.getAnnotation(this, Filters::class)
        val filterList = annotationTool.getAnnotations(this, Filter::class)
        
        return CoreFiltersAnnotationProcessor.process(FiltersAnnotationProcessContext(
            this,
            filters,
            filterList,
            listener,
            listenerAttributeMap,
            context
        ))
    }
    
    
    private fun KFunction<*>.interceptors(context: FunctionalListenerProcessContext): List<EventListenerInterceptor> {
        val annotations = annotationTool.getAnnotations(this, InterceptorAnnotation::class)
        if (annotations.isEmpty()) return emptyList()
        
        return annotations.map { it.toInterceptor(context) }.sortedBy { it.priority }
    }
    
    
    private fun InterceptorAnnotation.toInterceptor(context: FunctionalListenerProcessContext): EventListenerInterceptor {
        return when {
            value.isNotEmpty() -> {
                context.beanContainer[value, AnnotatedEventListenerInterceptor::class]
            }
            type != AnnotatedEventListenerInterceptor::class -> {
                val obj = type.objectInstance
                if (obj != null) return obj
                
                val beanContainer = context.beanContainer
                val allNamed = beanContainer.getAll(type)
                if (allNamed.isNotEmpty()) {
                    beanContainer[type]
                } else {
                    // try instance.
                    kotlin.runCatching {
                        instanceCache.computeIfAbsent(type) { type.createInstance() } as AnnotatedEventListenerInterceptor
                    }.getOrElse {
                        throw SimbotIllegalStateException(
                            "Cannot get an interceptor instance of type [$type]: does not exist in the bean container and cannot be instantiated directly: ${it.localizedMessage}",
                            it
                        )
                    }
                }
            }
            else ->
                throw SimbotIllegalStateException("@Interceptor needs to specify [value] or [type], and [value] cannot be empty or type cannot be equal to [AnnotatedEventListenerInterceptor.class] type self. But now the value is empty and the type is [AnnotatedEventListenerInterceptor.class].")
        }
    }
    
    
}


private data class ParameterBinderFactoryContextImpl(
    override val beanContainer: BeanContainer,
    override val source: KFunction<*>,
    override val parameter: KParameter,
) : ParameterBinderFactory.Context


/**
 * 将会直接抛出错误的binder。
 */
private class EmptyBinder(
    private val parameter: KParameter,
) : ParameterBinder {
    private val resultProvider: () -> Result<Any?> = when {
        parameter.isOptional -> {
            val ignoreResult: Result<Any?> = Result.success(ParameterBinder.Ignore)
            ({ ignoreResult })
        }
        parameter.type.isMarkedNullable -> {
            val nullResult: Result<Any?> = Result.success(null)
            ({ nullResult })
        }
        else -> ({
            Result.failure(BindException("Parameter(#${parameter.index}) [$parameter] has no binder."))
        })
    }
    
    override suspend fun arg(context: EventListenerProcessingContext): Result<Any?> {
        return resultProvider()
    }
}


/**
 * 组合多个binder的 [ParameterBinder].
 */
private class MergedBinder(
    private val binders: List<ParameterBinder>, // not be empty
    private val spare: List<ParameterBinder>, // empty able
    private val parameter: KParameter,
) : ParameterBinder {
    private companion object {
        val logger = LoggerFactory.getLogger(MergedBinder::class)
    }
    
    init {
        if (binders.isEmpty()) throw IllegalArgumentException("Binders cannot be empty.")
    }
    
    
    override suspend fun arg(context: EventListenerProcessingContext): Result<Any?> {
        var err: Throwable? = null
        val isOptional = parameter.isOptional
        
        suspend fun ParameterBinder.invoke(): Result<Any?>? {
            val result = arg(context)
            if (result.isSuccess) {
                // if success, return.
                return result
            }
            // failure
            val resultErr = result.exceptionOrNull()!!
            with(err) {
                if (this == null) {
                    err = resultErr
                } else {
                    addSuppressed(resultErr)
                }
            }
            return null
        }
        
        return kotlin.runCatching {
            for (binder in binders) {
                val result = binder.invoke()
                if (result != null) return result
            }
            for (binder in spare) {
                val result = binder.invoke()
                if (result != null) return result
            }
            if (isOptional) {
                if (logger.isTraceEnabled) {
                    logger.debug("Nothing binder success for listener(id={}).", context.listener.id)
                    logger.trace("Nothing binder success for listener(id=${context.listener.id})", err)
                } else {
                    logger.debug("Nothing binder success for listener(id={}). Enable trace level logging to view detailed reasons.",
                        context.listener.id)
                }
                return Result.success(ParameterBinder.Ignore)
            }
            
            Result.failure<Any?>(BindException("Nothing binder success for listener(id=${context.listener.id})", err))
        }.getOrElse { binderInvokeException ->
            err?.also {
                binderInvokeException.addSuppressed(it)
            }
            Result.failure(BindException("Binder invoke failure", binderInvokeException))
        }
    }
}