package net.wendal.nutzbook.module;

import java.util.Date;

import javax.servlet.http.HttpSession;

import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.QueryResult;
import org.nutz.dao.pager.Pager;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.nutz.mvc.annotation.At;
import org.nutz.mvc.annotation.Attr;
import org.nutz.mvc.annotation.By;
import org.nutz.mvc.annotation.Fail;
import org.nutz.mvc.annotation.Filters;
import org.nutz.mvc.annotation.Ok;
import org.nutz.mvc.annotation.Param;
import org.nutz.mvc.filter.CheckSession;

import net.wendal.nutzbook.bean.User;

@IocBean //还记得@IocBy吗？这个跟@IocBy有很大的关系
@At("/user")
@Ok("json:{locked:'password|salt',ignoreNull:true}")
@Fail("http:500")
@Filters(@By(type = CheckSession.class, args= {"me", "/"}))
public class UserModule {
	
	@Inject
	protected Dao dao; // 就这么注入了，有@IocBean它才会生效
	
	@At
	public int count() {
		return dao.count(User.class);
	}
	
	@At
	@Filters()
	public Object login(@Param("username") String name, @Param("password") String password, HttpSession session) {
		User user = dao.fetch(User.class, Cnd.where("name", "=", name).and("password", "=", password));
		if(user == null) {
			return false;
		} else {
			session.setAttribute("me", user.getId());
			return true;
		}
	}
	
	@At
	@Ok(">>:/")
	public void logout(HttpSession session) {
		session.invalidate();
	}
	
	@At
	public Object add(@Param("..")User user) {
		NutMap re = new NutMap();
		String msg = checkUser(user, true);
		if(msg != null) {
			return re.setv("ok", false).setv("msg", msg);
		}
		user.setCreateTime(new Date());
		user.setUpdateTime(new Date());
		user = dao.insert(user);
		return re.setv("ok", true).setv("data", user);
	}
	
	@At
	public Object update(@Param("..")User user) {
		NutMap re = new NutMap();
		String msg = checkUser(user, false);
		if(msg != null) {
			return re.setv("ok", false).setv("msg", msg);
		}
		user.setName(null); // 不允许更新用户名
		user.setCreateTime(null); // 不允许更新创建时间
		user.setUpdateTime(new Date()); // 
		dao.updateIgnoreNull(user);
		return re.setv("ok", true);
	}
	
	@At
	public Object delete(@Param("id")int id, @Attr("me")int me) {
		if(me==id) {
			return new NutMap().setv("ok", false).setv("msg", "不能删除当前用户！！！");
		}
		dao.delete(User.class, id); // 再严谨一些的话，需要判断是否为>0
		return new NutMap().setv("ok", true);
	}
	
	@At
	public Object query(@Param("name")String name, @Param("..")Pager pager) {
		Cnd cnd = Strings.isBlank(name) ? null : Cnd.where("name", "like", "%" + name + "%");
		QueryResult qr = new QueryResult();
		qr.setList(dao.query(User.class, cnd, pager));
		pager.setRecordCount(dao.count(User.class, cnd));
		qr.setPager(pager);
		return qr; // 默认分页是第1页，每页20条
	}
	
	protected String checkUser(User user, boolean create) {
		if(user == null) {
			return "空对象";
		}
		if(create) {
			if(Strings.isBlank(user.getName()) || Strings.isBlank(user.getPassword())) {
				return "用户名/密码不能为空";
			}
		} else {
			if(Strings.isBlank(user.getPassword())) {
				return "密码不能为空";
			}
		}
		String passwd = user.getPassword().trim();
		if(6>passwd.length() || passwd.length()>12) {
			return "密码长度错误";
		}
		user.setPassword(passwd);
		if(create) {
			int count = dao.count(User.class, Cnd.where("name", "=", user.getName()));
			if(count!=0) {
				return "用户名已经存在";
			}
		} else {
			if(user.getId()<1) {
				return "用户Id非法";
			}
		}
		if(user.getName()!=null) {
			user.setName(user.getName().trim());
		}
		return null;
	}
	
}
