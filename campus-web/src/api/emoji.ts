import request from './request'

export interface Emoji {
  id: number
  name: string
  emojiChar: string
  imageUrl: string
  tags: string
  category: string
}

export function getEmojiCategories() {
  return request.get<any, { code: number; data: string[] }>('/emoji/categories')
}

export function getEmojis(category?: string) {
  return request.get<any, { code: number; data: Emoji[] }>('/emoji', { params: { category } })
}

export function searchEmojis(keyword: string) {
  return request.get<any, { code: number; data: Emoji[] }>('/emoji/search', { params: { keyword } })
}

export function getFavoriteEmojis() {
  return request.get<any, { code: number; data: Emoji[] }>('/emoji/favorites')
}

export function toggleEmojiFavorite(id: number) {
  return request.post<any, { code: number; data: { favorited: boolean } }>(`/emoji/${id}/favorite`)
}
