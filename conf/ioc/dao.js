var ioc = {
	dataSource : {
		type : "com.alibaba.druid.pool.DruidDataSource",
		events : {
			create : "init",
			depose : "close"
		},
		fields : {
			url : "jdbc:mysql://127.0.0.1:3306/nutzbook",
			username : "root",
			password : "root",
			testWhileIdle : true, //非常重要，预防MySQL的8小时timeout问题
		}
	}
}
