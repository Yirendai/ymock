[TOC]

# ymock使用指南

## 功能介绍  
   ymock是一款面向单元测试与集成测试的辅助框架，其目的是解决测试过程中遇到的一些技术问题，方便测试进行，提高测试效率。它解决的问题主要为以下3个方面：1、隔离对第三方dubbo服务的依赖问题； 2、数据库数据准备问题；  3、按照一定规则创建测试数据的辅助功能。(仅限于jdk1.8+)

## 模块说明
   ymock-core对应代码实现，其MockJSTest对应着MockJS的使用举例；  
   ymock-demo主要是使用举例，其dubbo目录对应着mock dubbo的使用举例。test resources下面给出了测试时数据库配置的参考样例。

## 使用说明

### 依赖引入
```
<dependency>
    <groupId>com.yirendai.infra</groupId>
    <artifactId>ymock-core</artifactId>
    <version>0.0.3-SNAPSHOT</version>
</dependency
```

### mock Dubbo接口
* 添加配置文件处理bean，当改配置为true时，spring容器在启动时不会去检查dubbo registry是否连接正确，所有在采用mock时我们完全不依赖于第三方，包括zookeeper、dubbo provider。

```
<bean class="com.yirendai.infra.ymock.dubbo.DubboMockPostProcessor" >
       <!-- 是否开启Mock功能,true：开启，false：关闭 -->
       <constructor-arg index="0" value="true"/>
</bean>
```

* 对目标dubbo接口设定期望   

采用Jmockit语法
```
MockUp<PointFacade> pointService = new MockUp<PointFacade>() {
    @Mock
    public CommonResult<PointResponse> execSubPoint(PointRequest PointRequest)
    {
        CommonResult<PointResponse> pointResponseCommonResult = new CommonResult<>();
        pointResponseCommonResult.setCode("2009");
        return pointResponseCommonResult;
    }
};
 
DubboMock.set(pointService);
```

采用Mockito语法
```
PointRequest pointRequest = ...;
CommonResult<PointResponse> pointResponseCommonResult = new CommonResult<>();
pointResponseCommonResult.setCode("2008");
 
PointFacade pointFacade = mock(PointFacade.class);
when(pointFacade.execSubPoint(pointRequest)).thenReturn(pointResponseCommonResult);
 
DubboMock.set(pointFacade);
```

### 数据库数据准备
* 原则：  
1. 用真实的数据库，尽可能保证测试的有效性；  
2. 测试时构建最小测试数据集，方便数据管理，同时兼顾性能；  
* 方案：  
   数据库分为Dev数据库和Test数据库，Dev数据库仅用于开发，Test用于单元测试和集成测试  
* 实现原理：  
1. 将Dev数据库的表或数据（仅同步必须的）同步到Test数据库；  
2. 利用项目中已有的API对数据进行补充设置。   
* 使用说明

1. 配置
```
	<bean class="com.yirendai.infra.ymock.util.MysqlManager">
		<!-- 源地址 -->
		<constructor-arg index="0" ref="dataSource1" />
		<!-- 目标地址 -->
		<constructor-arg index="1" ref="dataSourceTest" />		
	</bean>
```

2. 根据需要同步指定的表和数据
```
    mysqlManager.syncTableSchema("table1"); // 设置需要同步表结构的表
    mysqlManager.syncTableData("table2");   // 设置需要同步表数据的表
    
    mysqlManager.syncCase();     //同步指定表的结构和数据.
    //mysqlManager.syncSuit();   //同步所有的表结构，同步指定表的数据.
```

### 测试数据准备(仅限JDK1.8+) 
* 支持[mockjs](http://mockjs.com/)语法
* 使用举例
```
    System.err.println(MockJS.mock("{\"string|1-10\":\"*\"}"));

    String exp = " {\"string|2-9\":\"*\"}";
    System.err.println(MockJS.mock(exp));
```
某次运行结果如下：
```
{"string":"****"}
{"string":"*****"}
```  