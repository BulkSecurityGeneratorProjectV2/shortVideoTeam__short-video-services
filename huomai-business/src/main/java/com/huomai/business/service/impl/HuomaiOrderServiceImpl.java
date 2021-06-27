package com.huomai.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.constant.WxPayConstants;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.huomai.business.bo.HuomaiOrderAddBo;
import com.huomai.business.bo.HuomaiOrderEditBo;
import com.huomai.business.bo.HuomaiOrderQueryBo;
import com.huomai.business.domain.HuomaiOrder;
import com.huomai.business.domain.HuomaiUser;
import com.huomai.business.mapper.HuomaiOrderMapper;
import com.huomai.business.service.IHuomaiOrderService;
import com.huomai.business.service.IHuomaiUserService;
import com.huomai.business.vo.HuomaiOrderDetailVo;
import com.huomai.business.vo.HuomaiOrderVo;
import com.huomai.common.core.domain.AjaxResult;
import com.huomai.common.core.page.TableDataInfo;
import com.huomai.common.exception.BaseException;
import com.huomai.common.utils.DateUtils;
import com.huomai.common.utils.PageUtils;
import com.huomai.common.utils.SecurityUtils;
import com.huomai.common.utils.ip.IpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * 订单记录Service业务层处理
 *
 * @author huomai
 * @date 2021-06-19
 */
@Service
public class HuomaiOrderServiceImpl extends ServiceImpl<HuomaiOrderMapper, HuomaiOrder> implements IHuomaiOrderService {

	@Autowired
	private HuomaiOrderMapper orderMapper;

	@Autowired
	private WxPayService wxService;

	@Autowired
	private IHuomaiUserService userService;


	@Override
	public HuomaiOrderVo queryById(Long id) {
		return getVoById(id, HuomaiOrderVo.class);
	}

	/***
	 * @description: 订单列表
	 * @author chenshufeng
	 * @date: 2021/6/26 9:47 下午
	 */
	@Override
	public TableDataInfo<HuomaiOrderVo> queryPageList(HuomaiOrderQueryBo bo) {
		List<HuomaiOrderVo> vos = orderMapper.queryList(PageUtils.buildPage(), bo);
		return PageUtils.buildDataInfo(vos);
	}

	@Override
	public List<HuomaiOrderVo> queryList(HuomaiOrderQueryBo bo) {
		return listVo(Wrappers.emptyWrapper(), HuomaiOrderVo.class);
	}


	@Override
	public Boolean insertByAddBo(HuomaiOrderAddBo bo) {
		HuomaiOrder add = BeanUtil.toBean(bo, HuomaiOrder.class);
		validEntityBeforeSave(add);
		return save(add);
	}

	@Override
	public Boolean updateByEditBo(HuomaiOrderEditBo bo) {
		HuomaiOrder update = BeanUtil.toBean(bo, HuomaiOrder.class);
		validEntityBeforeSave(update);
		return updateById(update);
	}

	/**
	 * 保存前的数据校验
	 *
	 * @param entity 实体类数据
	 */
	private void validEntityBeforeSave(HuomaiOrder entity) {
		//TODO 做一些数据校验,如唯一约束
	}

	@Override
	public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
		if (isValid) {
			//TODO 做一些业务上的校验,判断是否需要校验
		}
		return removeByIds(ids);
	}

	/***
	 * @description: 订单详情
	 * @author chenshufeng
	 * @date: 2021/6/26 9:51 下午
	 */
	@Override
	public HuomaiOrderDetailVo getInfo(Long id) {
		return getVoById(id, HuomaiOrderDetailVo.class);
	}

	/**
	 * 下单接口
	 *
	 * @param bo
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public AjaxResult createOrder(HuomaiOrderAddBo bo) {
		bo.setOrderNo(String.valueOf(System.currentTimeMillis()));

		String payWay = bo.getPayWay();
		//微信支付
		if ("1".equals(payWay)) {
			bo.setStatus("1");
			return wxPay(bo);
		} else if ("2".equals(payWay)) {
			//余额支付
			bo.setStatus("2");
			bo.setPayTime(DateUtils.getNowDate());
		}
		//插入订单记录
		insertByAddBo(bo);
		return AjaxResult.success();
	}

	/**
	 * 微信支付回调处理
	 *
	 * @param payNo
	 * @param bizPayNo
	 */
	@Override
	public void paySuccess(String payNo, String bizPayNo) {
		LambdaQueryWrapper<HuomaiOrder> wrapper = Wrappers.lambdaQuery();
		wrapper.eq(HuomaiOrder::getOrderNo, payNo);
		HuomaiOrder order = orderMapper.selectOne(wrapper);
		//已支付不处理
		if (order == null && order.getStatus().equals("2")) {
			return;
		}
		HuomaiOrder opt = new HuomaiOrder();
		opt.setId(order.getId());
		opt.setPayTime(DateUtils.getNowDate());
		opt.setStatus("2");//支付成功
		orderMapper.updateById(opt);
	}

	/**
	 * 调用微信支付api
	 *
	 * @param bo
	 * @return
	 */
	public AjaxResult wxPay(HuomaiOrderAddBo bo) {
		HuomaiUser user = userService.getById(SecurityUtils.getUserId());
		WxPayUnifiedOrderRequest orderRequest = new WxPayUnifiedOrderRequest();
		orderRequest.setBody("购买作品推广");
		orderRequest.setOutTradeNo(bo.getOrderNo());
		orderRequest.setTotalFee(bo.getAmount().multiply(BigDecimal.valueOf(100)).intValue());
		orderRequest.setSpbillCreateIp(IpUtils.getHostIp());
		orderRequest.setNotifyUrl(bo.getDomain() + "/wx/pay/callback");
		orderRequest.setTradeType(WxPayConstants.TradeType.JSAPI);
		orderRequest.setOpenid(user.getOpenid());
		try {
			return AjaxResult.success(wxService.createOrder(orderRequest));
		} catch (WxPayException e) {
			log.error(e.getMessage(), e);
			throw new BaseException("下单失败");
		}
	}
}
