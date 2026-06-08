import request from './request'

export interface CourseVO {
  id: number
  title: string
  description: string
  coverImage: string
  instructor: string
  category: string
  viewCount: number
  likeCount: number
  favoriteCount: number
  chapterCount: number
  liked: boolean
  favorited: boolean
  createdAt: string
}

export interface ChapterVO {
  id: number
  title: string
  description: string
  videoUrl: string
  duration: number
  sortOrder: number
}

export interface CourseDetailVO extends CourseVO {
  chapters: ChapterVO[]
}

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

export function getCourses(params: { category?: string; page?: number; size?: number }) {
  return request.get<any, { code: number; data: { total: number; records: CourseVO[] } }>(
    '/courses', { params }
  )
}

export function getCourseDetail(id: number) {
  return request.get<any, { code: number; data: CourseDetailVO }>(`/courses/${id}`)
}

export function likeCourse(id: number) {
  return request.post<any, { code: number; data: { liked: boolean } }>(`/courses/${id}/like`)
}

export function favoriteCourse(id: number) {
  return request.post<any, { code: number; data: { favorited: boolean } }>(`/courses/${id}/favorite`)
}

export function getFavoriteCourses(page = 1, size = 12) {
  return request.get<any, { code: number; data: { total: number; records: CourseVO[] } }>(
    '/courses/favorites', { params: { page, size } }
  )
}

export function getComments(courseId: number, sort = 'hot') {
  return request.get<any, { code: number; data: CommentVO[] }>(
    `/courses/${courseId}/comments`, { params: { sort } }
  )
}

export function addComment(courseId: number, content: string, parentId?: number, replyToUserId?: number) {
  return request.post<any, { code: number; data: CommentVO }>(
    `/courses/${courseId}/comments`, { content, parentId: parentId?.toString(), replyToUserId: replyToUserId?.toString() }
  )
}

export function likeComment(commentId: number) {
  return request.post<any, { code: number; data: { liked: boolean } }>(`/courses/comments/${commentId}/like`)
}

export function deleteComment(commentId: number) {
  return request.delete(`/courses/comments/${commentId}`)
}
