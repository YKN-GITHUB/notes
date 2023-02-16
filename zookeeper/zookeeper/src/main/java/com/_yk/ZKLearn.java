package com._yk;

import cn.hutool.core.util.RuntimeUtil;

public class ZKLearn {

    /**
     * zk是有session概念的，没有连接池的概念
     * 每个连接拥有一个独立的session
     * 创建连接传入的 watch 是session级别的，跟path、node没有关系
     * watch 只发生在get、exits。。。方法中的
     * watch 是一次性的
     * zk有两套API，reactive模型与同步模型的
     */

    // 创建连接
    public void connect(){
    }

}
