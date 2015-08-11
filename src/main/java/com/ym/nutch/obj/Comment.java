package com.ym.nutch.obj;

/**
 * 用于保存评论内容
 * @author Administrator
 *
 */
public class Comment {

	private String commentId;
	private String commentContent;
	
	public String getCommentId() {
		return commentId;
	}
	public void setCommentId(String commentId) {
		this.commentId = commentId;
	}
	public String getCommentContent() {
		return commentContent;
	}
	public void setCommentContent(String commentContent) {
		this.commentContent = commentContent;
	}
}
