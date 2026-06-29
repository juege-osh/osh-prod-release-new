const API_BASE = import.meta.env.VITE_API_BASE || '/api'

export function getToken() {
  return localStorage.getItem('osh_release_token') || ''
}

export function setToken(token) {
  if (token) {
    localStorage.setItem('osh_release_token', token)
  } else {
    localStorage.removeItem('osh_release_token')
  }
}

export async function api(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }
  const token = getToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  })
  const payload = await response.json().catch(() => ({ success: false, message: '响应不是 JSON' }))
  if (!response.ok || payload.success === false) {
    throw new Error(payload.message || `请求失败：${response.status}`)
  }
  return payload.data
}

export function post(path, body = {}) {
  return api(path, {
    method: 'POST',
    body: JSON.stringify(body)
  })
}
