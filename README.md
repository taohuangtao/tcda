# tdull-commons-db-ar

目前只支持mysql



## 快速开始
查询 cms_column 表所有数据
```
DbHelper dbHelper = new DbHelper();
dbHelper.setDataSource(dataSource);//配置数据源
Model column = dbHelper.getModelInstance("cms_column");//cms_column配置需要查询的表
List<Map<String, Object>> list = column.filter("*").select();//获取所有数据
```