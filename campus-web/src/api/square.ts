import request from './request'

export interface SquarePost {
  id: number
  userId: number
  userNickname: string
  userAvatar: string
  title: string
  question: string
  answer: string
  referencesJson: string
  category: string
  viewCount: number
  likeCount: number
  liked: boolean
  favorited: boolean
  createdAt: string
}

export interface PublishRequest {
  title: string
  question: string
  answer: string
  referencesJson?: string
  category?: string
}

export function getSquarePosts(params: { category?: string; keyword?: string; page?: number; size?: number }) {
  return request.get<any, { code: number; data: { total: number; records: SquarePost[] } }>(
    '/square', { params }
  )
}

export function getHotPosts(params: { page?: number; size?: number }) {
  const { page = 1, size = 10 } = params
  return request.get<any, { code: number; data: { total: number; records: SquarePost[] } }>(
    '/square/hot', { params: { page, size } }
  )
}

export function getSquarePostDetail(id: number) {
  return request.get<any, { code: number; data: SquarePost }>(`/square/${id}`)
}

export function publishToSquare(data: PublishRequest) {
  return request.post('/square/publish', data)
}

export function likeSquarePost(id: number) {
  return request.post<any, { code: number; data: { liked: boolean } }>(`/square/${id}/like`)
}

export function favoriteSquarePost(id: number) {
  return request.post<any, { code: number; data: { favorited: boolean } }>(`/square/${id}/favorite`)
}

export function getMyPosts(page = 1, size = 10) {
  return request.get<any, { code: number; data: { total: number; records: SquarePost[] } }>(
    '/square/my', { params: { page, size } }
  )
}

// 评论
export interface CommentVO {
  id: number
  userId: number
  userNickname: string
  userAvatar: string
  replyToUserId: number | null
  replyToUserNickname: string | null
  content: string
  likeCount: number
  liked: boolean
  createdAt: string
  replies: CommentVO[] | null
}

export function getComments(postId: number, sort = 'hot') {
  return request.get<any, { code: number; data: CommentVO[] }>(`/square/${postId}/comments`, { params: { sort } })
}

export function addComment(postId: number, content: string, parentId?: number, replyToUserId?: number) {
  return request.post<any, { code: number; data: CommentVO }>(`/square/${postId}/comments`, {
    content, parentId: parentId?.toString(), replyToUserId: replyToUserId?.toString()
  })
}

export function likeComment(commentId: number) {
  return request.post<any, { code: number; data: { liked: boolean } }>(`/square/comments/${commentId}/like`)
}

export function deleteComment(commentId: number) {
  return request.delete(`/square/comments/${commentId}`)
}
