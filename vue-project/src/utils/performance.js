/**
 * 性能优化工具函数
 * 提供各种性能优化相关的工具函数
 */

/**
 * 防抖函数
 * 用于限制函数执行频率，常用于搜索框输入、窗口调整等场景
 * @param {Function} fn - 要执行的函数
 * @param {number} delay - 延迟时间（毫秒）
 * @returns {Function} 防抖后的函数
 */
export function debounce(fn, delay = 300) {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

/**
 * 节流函数
 * 用于限制函数执行频率，常用于滚动事件、鼠标移动等场景
 * @param {Function} fn - 要执行的函数
 * @param {number} limit - 限制时间（毫秒）
 * @returns {Function} 节流后的函数
 */
export function throttle(fn, limit = 300) {
  let inThrottle = false
  return function (...args) {
    if (!inThrottle) {
      fn.apply(this, args)
      inThrottle = true
      setTimeout(() => (inThrottle = false), limit)
    }
  }
}

/**
 * 懒加载图片
 * 使用 Intersection Observer API 实现图片懒加载
 * @param {string} selector - 图片选择器
 * @param {Object} options - 配置选项
 */
export function lazyLoadImages(selector = 'img[data-src]', options = {}) {
  const defaultOptions = {
    root: null,
    rootMargin: '0px 0px 50px 0px',
    threshold: 0.01
  }

  const observerOptions = { ...defaultOptions, ...options }

  const imageObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        const img = entry.target
        const src = img.getAttribute('data-src')

        if (src) {
          img.src = src
          img.removeAttribute('data-src')
          img.classList.add('loaded')
        }

        observer.unobserve(img)
      }
    })
  }, observerOptions)

  const images = document.querySelectorAll(selector)
  images.forEach((img) => imageObserver.observe(img))

  return imageObserver
}

/**
 * 预加载图片
 * 提前加载图片资源
 * @param {string[]} urls - 图片 URL 数组
 * @returns {Promise} 加载完成的 Promise
 */
export function preloadImages(urls) {
  const promises = urls.map((url) => {
    return new Promise((resolve, reject) => {
      const img = new Image()
      img.onload = resolve
      img.onerror = reject
      img.src = url
    })
  })

  return Promise.all(promises)
}

/**
 * 测量性能指标
 * 使用 Performance API 测量页面性能
 */
export function measurePerformance() {
  // 等待页面完全加载
  window.addEventListener('load', () => {
    // 使用 requestIdleCallback 在浏览器空闲时执行
    if ('requestIdleCallback' in window) {
      requestIdleCallback(() => {
        const perfData = window.performance.timing

        // 计算关键性能指标
        const metrics = {
          // DNS 查询时间
          dns: perfData.domainLookupEnd - perfData.domainLookupStart,
          // TCP 连接时间
          tcp: perfData.connectEnd - perfData.connectStart,
          // 首字节时间 (TTFB)
          ttfb: perfData.responseStart - perfData.requestStart,
          // DOM 解析时间
          domParse: perfData.domComplete - perfData.domLoading,
          // 页面完全加载时间
          loadComplete: perfData.loadEventEnd - perfData.navigationStart
        }

        console.log('📊 性能指标:', metrics)

        // 可以发送到性能监控服务
        // sendToAnalytics(metrics)
      })
    }
  })
}

/**
 * 使用 requestIdleCallback 执行低优先级任务
 * @param {Function} callback - 回调函数
 * @param {Object} options - 配置选项
 */
export function runWhenIdle(callback, options = { timeout: 2000 }) {
  if ('requestIdleCallback' in window) {
    return requestIdleCallback(callback, options)
  } else {
    // 降级方案：使用 setTimeout
    return setTimeout(callback, 1)
  }
}

/**
 * 使用 requestAnimationFrame 执行动画
 * @param {Function} callback - 回调函数
 */
export function raf(callback) {
  return requestAnimationFrame(callback)
}

/**
 * 批量处理大量数据
 * 将大数据集分批处理，避免阻塞主线程
 * @param {Array} items - 数据项数组
 * @param {Function} processor - 处理函数
 * @param {number} batchSize - 每批处理数量
 * @returns {Promise} 处理完成的 Promise
 */
export function processInBatches(items, processor, batchSize = 100) {
  return new Promise((resolve) => {
    const results = []
    let index = 0

    function processBatch() {
      const batch = items.slice(index, index + batchSize)

      if (batch.length === 0) {
        resolve(results)
        return
      }

      batch.forEach((item) => {
        results.push(processor(item))
      })

      index += batchSize

      // 使用 requestIdleCallback 让出主线程
      runWhenIdle(processBatch)
    }

    processBatch()
  })
}

/**
 * 缓存函数结果（记忆化）
 * @param {Function} fn - 要缓存的函数
 * @returns {Function} 带缓存的函数
 */
export function memoize(fn) {
  const cache = new Map()

  return function (...args) {
    const key = JSON.stringify(args)

    if (cache.has(key)) {
      return cache.get(key)
    }

    const result = fn.apply(this, args)
    cache.set(key, result)
    return result
  }
}

/**
 * 监听长任务
 * 使用 PerformanceObserver 监听长任务
 */
export function observeLongTasks() {
  if ('PerformanceObserver' in window) {
    const observer = new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        // 如果任务超过 50ms，认为是长任务
        if (entry.duration > 50) {
          console.warn('⚠️ 发现长任务:', {
            duration: entry.duration,
            startTime: entry.startTime
          })
        }
      }
    })

    observer.observe({ entryTypes: ['longtask'] })
    return observer
  }
}

/**
 * 资源预加载提示
 * 使用 <link rel="preload"> 预加载关键资源
 * @param {string} href - 资源地址
 * @param {string} as - 资源类型
 */
export function preloadHint(href, as = 'script') {
  const link = document.createElement('link')
  link.rel = 'preload'
  link.href = href
  link.as = as

  if (as === 'font') {
    link.crossOrigin = 'anonymous'
  }

  document.head.appendChild(link)
}

/**
 * 延迟加载非关键资源
 * 使用 <link rel="prefetch"> 预取未来可能需要的资源
 * @param {string} href - 资源地址
 */
export function prefetchResource(href) {
  const link = document.createElement('link')
  link.rel = 'prefetch'
  link.href = href
  document.head.appendChild(link)
}

/**
 * 初始化性能监控
 */
export function initPerformanceMonitoring() {
  // 测量性能指标
  measurePerformance()

  // 监听长任务
  observeLongTasks()

  // 监听页面可见性变化
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
      // 页面不可见时，可以暂停一些任务
      console.log('📱 页面切换到后台')
    } else {
      // 页面可见时，恢复任务
      console.log('📱 页面切换到前台')
    }
  })
}
