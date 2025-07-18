const fetchMapCenter = async () => {
  try {
    const response = await axios.get('/api-status/lane-mappings')
    const mappings = Array.isArray(response.data) ? response.data : Object.values(response.data)

    let minX = Infinity, maxX = -Infinity
    mappings.forEach((m: any) => {
      if (m.laneShape) {
        const coordinates = m.laneShape.trim().split(' ').map((p: string) => p.split(',').map(Number))
        coordinates.forEach((coord: number[]) => {
          minX = Math.min(minX, coord[0])
          maxX = Math.max(maxX, coord[0])
        })
      }
    })

    mapCenterX.value = (minX + maxX) / 2
    console.log('🗺️ [Manual] Map center X:', mapCenterX.value)
  } catch (error) {
    console.error(' [Manual] Failed to fetch map center:', error)
  }
}

const fetchJunctions = async () => {
  try {
    console.log('📡 [Manual] Fetching junctions...')

    const response = await axios.get('/api-status/junctions')
    const junctionData = Object.values(response.data)

    const coordResponse = await axios.get('/api-status/tls-junctions')
    const coordData = coordResponse.data


    const coordMap = new Map()
    coordData.forEach((item: any) => {
      coordMap.set(item.junctionId, {
        junctionX: item.junctionX,
        junctionY: item.junctionY
      })
    })

    console.log('📊 [Manual] Raw junction data count:', junctionData.length)

    junctionDataList.value = junctionData.map((junction: any) => {
      const coords = coordMap.get(junction.junction_id) || { junctionX: 0, junctionY: 0 }
      return {
        tlsID: junction.tlsID || junction.tlsId,
        junction_id: junction.junction_id,
        junction_name: junction.junction_name,
        connection: junction.connection || [],
        junctionX: coords.junctionX,
        junctionY: coords.junctionY
      }
    })

    console.log('✅ [Manual] Processed junctions:', junctionDataList.value.length)
    console.log('🔍 [Manual] Sample junction:', junctionDataList.value[0])

  } catch (error) {
    console.error('❌ [Manual] Failed to fetch junctions:', error)
  }
}

const onApply = async () => {
  const junction = currentJunction.value
  const lightIndex = selectedDirectionIndex.value

  if (!junction || lightIndex === null || duration.value === null) {
    return
  }

  if (!canApplyManualControl({ junctionX: junction.junctionX || 0, junctionY: junction.junctionY || 0 }, mapCenterX.value)) {
    toast.error(PERMISSION_MESSAGES.TRAFFIC_LIGHT_MODIFY_DENIED)
    return
  }

  isApplying.value = true
  const state = selectedLight.value === 'GREEN' ? 'G' : 'r'
  const requestBody = {
    junctionId: junction.junction_id,
    lightIndex,
    duration: duration.value,
    state,
    source: 'manual'
  }

  const currentDirection = currentDirections.value[lightIndex]
  const fromEdge = currentDirection?.fromEdgeName || 'Unknown'
  const toEdge = currentDirection?.toEdgeName || 'Unknown'
  const lightColor = selectedLight.value === 'GREEN' ? 'Green' : 'Red'
  const junctionName = junction.junction_name || junction.junction_id

  const recordId = operationStore.addRecord({
    description: `Set ${junctionName} light from ${fromEdge} to ${toEdge} to ${lightColor} for ${duration.value}s`,
    source: 'manual',
    junctionId: junction.junction_id,
    junctionName,
    lightIndex,
    state,
    duration: duration.value
  })

  try {
    await apiClient.post('/signalcontrol/manual', requestBody)
    operationStore.updateRecordStatus(recordId, 'success')
    toast.success('Traffic light settings updated successfully!')

    const directionInfo = `${fromEdge} → ${toEdge}`
    emit('manualControlApplied', {
      junctionName,
      directionInfo,
      lightColor,
      duration: duration.value
    })

    partialResetForm()
  } catch (error) {
    console.error('发送控制请求失败:', error)
    operationStore.updateRecordStatus(recordId, 'failed', 'Failed to send data to backend')
    toast.error('Failed to send data to backend.')
  } finally {
    isApplying.value = false
  }
}

const increaseDuration = () => {
  if (!canModifyLights.value) return

  if (duration.value === null) duration.value = 5
  else if (duration.value < 300) duration.value++
  durationDisplay.value = duration.value.toString()
  durationError.value = false
}

const decreaseDuration = () => {
  if (!canModifyLights.value) return

  if (duration.value && duration.value > 5) {
    duration.value--
  } else {
    duration.value = 5
  }
  durationDisplay.value = duration.value.toString()
  durationError.value = false
}

onMounted(async () => {
  await fetchMapCenter()
  await fetchLaneMappings()
  await fetchJunctions()
  document.addEventListener('click', handleClickOutside)
})
