package com.huomai.web.controller.system;

import com.huomai.common.annotation.Log;
import com.huomai.common.constant.UserConstants;
import com.huomai.common.core.controller.BaseController;
import com.huomai.common.core.domain.AjaxResult;
import com.huomai.common.core.domain.entity.SysDictType;
import com.huomai.common.core.page.TableDataInfo;
import com.huomai.common.enums.BusinessType;
import com.huomai.common.utils.SecurityUtils;
import com.huomai.common.utils.poi.ExcelUtil;
import com.huomai.system.service.ISysDictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据字典信息
 *
 * @author huomai
 */
@RestController
@RequestMapping("/system/dict/type")
public class SysDictTypeController extends BaseController {
	@Autowired
	private ISysDictTypeService dictTypeService;

	@PreAuthorize("@ss.hasPermi('system:dict:list')")
	@GetMapping("/list")
	public TableDataInfo list(SysDictType dictType) {
		return dictTypeService.selectPageDictTypeList(dictType);
	}

	@Log(title = "字典类型", businessType = BusinessType.EXPORT)
	@PreAuthorize("@ss.hasPermi('system:dict:export')")
	@GetMapping("/export")
	public AjaxResult export(SysDictType dictType) {
		List<SysDictType> list = dictTypeService.selectDictTypeList(dictType);
		ExcelUtil<SysDictType> util = new ExcelUtil<SysDictType>(SysDictType.class);
		return util.exportExcel(list, "字典类型");
	}

	/**
	 * 查询字典类型详细
	 */
	@PreAuthorize("@ss.hasPermi('system:dict:query')")
	@GetMapping(value = "/{dictId}")
	public AjaxResult getInfo(@PathVariable Long dictId) {
		return AjaxResult.success(dictTypeService.selectDictTypeById(dictId));
	}

	/**
	 * 新增字典类型
	 */
	@PreAuthorize("@ss.hasPermi('system:dict:add')")
	@Log(title = "字典类型", businessType = BusinessType.INSERT)
	@PostMapping
	public AjaxResult add(@Validated @RequestBody SysDictType dict) {
		if (UserConstants.NOT_UNIQUE.equals(dictTypeService.checkDictTypeUnique(dict))) {
			return AjaxResult.error("新增字典'" + dict.getDictName() + "'失败，字典类型已存在");
		}
		dict.setCreateBy(SecurityUtils.getUsername());
		return toAjax(dictTypeService.insertDictType(dict));
	}

	/**
	 * 修改字典类型
	 */
	@PreAuthorize("@ss.hasPermi('system:dict:edit')")
	@Log(title = "字典类型", businessType = BusinessType.UPDATE)
	@PutMapping
	public AjaxResult edit(@Validated @RequestBody SysDictType dict) {
		if (UserConstants.NOT_UNIQUE.equals(dictTypeService.checkDictTypeUnique(dict))) {
			return AjaxResult.error("修改字典'" + dict.getDictName() + "'失败，字典类型已存在");
		}
		dict.setUpdateBy(SecurityUtils.getUsername());
		return toAjax(dictTypeService.updateDictType(dict));
	}

	/**
	 * 删除字典类型
	 */
	@PreAuthorize("@ss.hasPermi('system:dict:remove')")
	@Log(title = "字典类型", businessType = BusinessType.DELETE)
	@DeleteMapping("/{dictIds}")
	public AjaxResult remove(@PathVariable Long[] dictIds) {
		dictTypeService.deleteDictTypeByIds(dictIds);
		return success();
	}

	/**
	 * 刷新字典缓存
	 */
	@PreAuthorize("@ss.hasPermi('system:dict:remove')")
	@Log(title = "字典类型", businessType = BusinessType.CLEAN)
	@DeleteMapping("/refreshCache")
	public AjaxResult refreshCache() {
		dictTypeService.resetDictCache();
		return AjaxResult.success();
	}

	/**
	 * 获取字典选择框列表
	 */
	@GetMapping("/optionselect")
	public AjaxResult optionselect() {
		List<SysDictType> dictTypes = dictTypeService.selectDictTypeAll();
		return AjaxResult.success(dictTypes);
	}
}
