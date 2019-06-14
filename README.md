# TCDA

## 简介
轻量级的orm工具,实现ActiveRecord模式、连贯操作和统计查询.简化sql脚本编写，和其他类似orm工具复炸的配置文件和接口文件等，大大简化数据库操作，用最少的代码做最多的事。  
抛弃其他框架繁琐的xml语法，大量的常规操作都不需要写sql语句，极大减轻宽表的插入和跟新操作。 
核心文件就只有一个，没有多余的方法，只有where,filter,select,insert,orderBy,groupBy,update,delete核心方法。
最少的依赖，只有日志系统依赖，无其他第三方依赖。

* 目前只支持mysql  
* 默认支持数据库小写加下划线命名方式和java bean映射方式    user_name > userName  ,直接驼峰法也可以。

## mvn
```
<!-- https://mvnrepository.com/artifact/com.tdull.commons.db/tdull-commons-db-ar -->
<dependency>
    <groupId>com.tdull.commons.db</groupId>
    <artifactId>tdull-commons-db-ar</artifactId>
    <version>1.2.12</version>
</dependency>
```


## 快速开始  
### 配置数据源  
DbHelper dbHelper = new DbHelper();
dbHelper.setDataSource(dataSource);//配置数据源,可以使用你项目的连接池的对象，也可以利用spring进行注入
### 查询 cms_column 表所有数据  
```
//cms_column配置需要查询的表 下面的操作都在这个表上面进行
Model column = dbHelper.getModelInstance("cms_column");

//获取所有数据 Map保存数据
List<Map<String, Object>> list = column.filter("*").select();

//获取所有数据 将数据转换为实体
List<ColumnPo> list = column.filter("*").select(ColumnPo.class);
```
### while查询
```
//cms_column配置需要查询的表
Model column = dbHelper.getModelInstance("cms_column");

//while支持常用的  >、<、!= 、<>、like,in、not in、BETWEEN
Map<String,Object> where = new HashMap<>();
// 等于查询 ，其他单值条件相同的方式 ，只是KEY不同     查询ID等于4
where.put("id",4);
//不等于  查询id不等于3
where.put("id",Arrays.asList("!=",3));
//LIKE查询 name like '%张三%'
where.put("id",Arrays.asList("LIKE",'%张三%'));
//包含查询 查询id包含在 1，2，3，4，5  ，IN和NOT IN 相同
where.put("id",Arrays.asList("IN",Arrays.asList(1,2,3,4,5)));
//查询区间 id 大于1小于100
where.put("id",Arrays.asList("BETWEEN",Arrays.asList(1,100)));
column.where(where).select();
```
