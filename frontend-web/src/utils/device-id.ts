const KEY = 'vm_device_id'

export function getDeviceId(): string {
  let id = localStorage.getItem(KEY)
  if (!id) {
    id = crypto.randomUUID?.() ?? 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
      const r = Math.random() * 16 | 0
      return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16)
    })
    localStorage.setItem(KEY, id)
  }
  return id
}
