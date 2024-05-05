package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author XIZAI
 * @since 2021-12-22
 */
public interface IShopTypeService extends IService<ShopType> {

    /**
     * List 缓存
     * @return
     */
    public Result queryTypeList();

    /**
     * String 缓存
     * @return
     */
    public Result queryTypeString();
}
