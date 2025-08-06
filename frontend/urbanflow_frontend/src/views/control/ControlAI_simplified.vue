// 这是简化版的关键console.log替换指南
// 请在原文件中按照以下替换：

// 1. 删除所有包含emoji和中文的console.log
// 2. 替换为以下简化的英文日志：

// 原: console.log('🔄 getNextSuggestionInCycle 开始，当前池大小:', ...)
// 改为: console.log('AI: Getting next suggestion, pool size:', currentBatchSuggestions.value.length)

// 原: console.log('🔄 已处理建议数量:', ...)  
// 改为: console.log('AI: Processed suggestions count:', processedSuggestions.value.size)

// 原: console.log('📥 当前批次为空，获取新批次建议')
// 改为: console.log('AI: Current batch empty, fetching new batch')

// 原: console.log('⚠️ 没有可用的建议')
// 改为: console.log('AI: No available suggestions')

// 原: console.log('🔄 循环显示: 找到未处理建议...')
// 改为: console.log('AI: Found unprocessed suggestion')

// 原: console.log('🔄 当前批次所有建议已处理，尝试获取新建议')
// 改为: console.log('AI: All suggestions processed, getting new batch')

// 原: console.log('🆕 获取到新建议，显示第一个')
// 改为: console.log('AI: Got new suggestions, showing first one')

// 原: console.log('🔄 后端无新建议且所有建议已处理...')
// 改为: console.log('AI: No new suggestions available')

// 原: console.error('循环获取建议失败:', error)
// 改为: console.error('AI: Failed to get next suggestion:', error)

// 原: console.log('🔄 定时检查（每10秒）：获取新批次建议数据')
// 改为: console.log('AI: Batch refresh timer check')

// 原: console.error('🔄 批次刷新失败:', error)
// 改为: console.error('AI: Batch refresh failed:', error)

// 原: console.log('🔄 重新启动批次刷新定时器')
// 改为: console.log('AI: Restarting batch refresh timer')

// 原: console.log('🔄 批次刷新定时器已启动，间隔:', ...)
// 改为: console.log('AI: Batch refresh timer started, interval:', BATCH_REFRESH_INTERVAL, 'ms')

// 原: console.log('🔄 批次数据未变化，跳过更新')
// 改为: console.log('AI: Batch data unchanged, skipping update')

// 原: console.log('🆕 新增...个建议到建议池...')
// 改为: console.log('AI: Added new suggestions to pool, total:', currentBatchSuggestions.value.length)

// 原: console.log('🔄 没有新的唯一建议，保持现有池')
// 改为: console.log('AI: No new unique suggestions')

// 原: console.log('⚠️ 后端返回空建议列表')
// 改为: console.log('AI: Backend returned empty suggestions')

// 原: console.error('批次刷新失败:', error)
// 改为: console.error('AI: Batch refresh failed:', error)

// 原: console.log('🔄 强制刷新：重新获取数据')
// 改为: console.log('AI: Force refresh - getting new data')

// 原: console.log('🔄 循环模式：显示下一个建议')
// 改为: console.log('AI: Cycle mode - showing next suggestion')

// 原: console.log('✅ 建议已加载:', {...})
// 改为: console.log('AI: Suggestion loaded')

// 原: console.log('⚠️ 没有可用的建议')
// 改为: console.log('AI: No available suggestions')

// 原: console.error('获取建议失败:', error)
// 改为: console.error('AI: Failed to get suggestions:', error)

// 原: console.log('🔄 10秒循环：显示下一个建议')
// 改为: console.log('AI: 10s cycle - showing next suggestion')

// 原: console.log('🔄 无建议，5秒后强制刷新')
// 改为: console.log('AI: No suggestions, force refresh in 5s')

// 原: console.log('Switching to AI mode - Auto-apply enabled')
// 改为: console.log('AI: Switching to AI mode - Auto-apply enabled')

// 原: console.log('Switching to Manual mode - Auto-apply disabled')
// 改为: console.log('AI: Switching to Manual mode - Auto-apply disabled')

// 原: console.log('AI suggestion auto-applied successfully')
// 改为: console.log('AI: Suggestion auto-applied successfully')

// 原: console.error('AI suggestion auto-apply failed')
// 改为: console.error('AI: Suggestion auto-apply failed')

// 原: console.log('✅ 建议已应用并标记为已处理:', ...)
// 改为: console.log('AI: Suggestion applied and marked as processed')

// 原: console.log('✅ 当前已处理建议数量:', ...)
// 改为: console.log('AI: Current processed suggestions count:', processedSuggestions.value.size)

// 原: console.log('❌ 建议已忽略并标记为已处理:', ...)
// 改为: console.log('AI: Suggestion ignored and marked as processed')

// 原: console.log('❌ 当前已处理建议数量:', ...)
// 改为: console.log('AI: Current processed suggestions count:', processedSuggestions.value.size)

// 原: console.log('🚀 组件初始化开始')
// 改为: console.log('AI: Component initialization started')

// 原: console.log('🚀 缓存初始化完成，开始获取建议')
// 改为: console.log('AI: Cache initialized, starting to get suggestions')

// 原: console.log('🚀 立即启动批次刷新定时器')
// 改为: console.log('AI: Starting batch refresh timer immediately')

// 删除其他所有调试日志和不必要的console.log
