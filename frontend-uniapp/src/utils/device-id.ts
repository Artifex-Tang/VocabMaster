import { storage } from '@/utils/storage'

export function getDeviceId(): string {
  let id = storage.get<string>('device_id')
  if (!id) {
    id = generateId()
    storage.set('device_id', id)
  }
  return id
}

function generateId(): string {
  const sys = uni.getSystemInfoSync()
  const seed = `${sys.platform}-${sys.model ?? 'x'}-${Date.now()}-${Math.random()}`
  let h = 0
  for (let i = 0; i < seed.length; i++) {
    h = ((h << 5) - h + seed.charCodeAt(i)) | 0
  }
  return `d_${Math.abs(h).toString(36)}_${Date.now().toString(36)}`
}
