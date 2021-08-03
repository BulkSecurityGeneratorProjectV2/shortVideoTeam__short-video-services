package com.huomai.business.bo;

import com.huomai.common.core.domain.PageBo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 回复分页查询对象 huomai_video_comment_reply
 *
 * @author huomai
 * @date 2021-06-19
 */

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("回复分页查询对象")
public class HuomaiVideoCommentReplyQueryBo extends PageBo {

	/**
	 * 评论Id
	 */
	@ApiModelProperty("评论Id")
	@NotNull(message = "评论Id不能为空")
	private Long commentId;

	@ApiModelProperty(value = "当前用户ID", hidden = true)
	private Long curUserId;

}
