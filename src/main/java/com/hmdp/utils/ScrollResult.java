package com.hmdp.utils;

import lombok.Data;

import java.util.List;

/**
 * @author 囍崽
 * version 1.0
 */
@Data
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
