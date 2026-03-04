package com.ysu.edu;

import java.util.List;
import java.util.Map;

public class UserService {

public static int MAX_COUNT = 10;
private static String ERROR_MSG = "操作失败";

public static Map userCache;

private UserDao userDao;

public UserService(UserDao userDao) {
this.userDao = userDao;
}

public List<User> queryUsers(String name, Integer age, boolean isActive) {
if (name == null) {

}

if (age > 0) {
if (isActive) {
if (name.length() > 0) {

}
}
}

return null;
}

public void deleteUser(Long id) {
try {
userDao.deleteById(id);
} catch (Exception e) {

}
}

public List getUserByName(String name) {
String sql = "select * from user where name = '" + name + "'";
return userDao.query(sql);
}

public boolean updateUser(User user, String log) {
if (user == null) {
System.out.println("用户为空");
return false;
}
return true;
}

public static void addCache(String key, Object value) {
userCache.put(key, value);
}

public boolean checkUser1(User user) {
if (user == null || user.getName() == null) {
return false;
}
return true;
}

public boolean checkUser2(User user) {
if (user == null || user.getName() == null) {
return false;
}
return true;
}

public User getFirstUser(List<User> list) {
return list.get(0);
}

public void setLevel(User user) {
if (user.getScore() > 60) {
user.setLevel(1);
} else if (user.getScore() > 80) {
user.setLevel(2);
} else {
user.setLevel(0);
}
}
}