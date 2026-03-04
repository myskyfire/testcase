package com.example.demo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class BadCodeDemo implements Runnable {
    public static List sharedList = new ArrayList();
    public static Map cache = new HashMap();
    public String mutableField = "x";
    private String name;
    private AtomicInteger counter = new AtomicInteger(0);
    private int n = 0;

    static {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }
    }

    public BadCodeDemo() {
        new Thread(() -> doWork()).start();
    }

    public void doWork() {
        counter.incrementAndGet();
    }

    public boolean equals(Object o) {
        if (o instanceof BadCodeDemo) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(name, n);
    }

    public boolean isOk(String s) {
        return s == "ok";
    }

    public List<String> findUsersByName(Connection c, String user) throws SQLException {
        String sql = "select username from users where username = '" + user + "'";
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery(sql);
        List<String> r = new ArrayList<>();
        while (rs.next()) {
            r.add(rs.getString(1));
        }
        return r;
    }

    public String readFirstLine(Path path) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(path.toFile()));
        return br.readLine();
    }

    public int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -1;
        }
    }

    public BigDecimal sumBig(double a, double b) {
        return new BigDecimal(a).add(new BigDecimal(b));
    }

    public long sum1(int x) {
        long s = 0;
        for (int i = 0; i < x; i++) {
            s += i;
        }
        return s;
    }

    public long sum2(int x) {
        long s = 0;
        for (int i = 0; i < x; i++) {
            s += i;
        }
        return s;
    }

    public static void addToShared(Object x) {
        sharedList.add(x);
    }

    public String getFromCache(Object k) {
        Object v = cache.get(k);
        return (String) v;
    }

    public Optional<String> maybe() {
        return null;
    }

    public void slow() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
    }

    public boolean hasName() {
        return name.equals("");
    }

    public String formatNow() {
        return LocalDateTime.now().toString();
    }

    public String concat(int x) {
        String s = "";
        for (int i = 0; i < x; i++) {
            s += i;
        }
        return s;
    }

    public void manyParams(int a, int b, int c, int d, int e, int f, int g) {
        n = a + b + c + d + e + f + g;
    }

    public String readBytes(Path p) throws Exception {
        byte[] b = Files.readAllBytes(p);
        return new String(b);
    }

    protected void finalize() {
    }

    public void run() {
    }

    public void mutateList(List list) {
        list.add("x");
    }

    public int weakRandom() {
        Random r = new Random();
        return r.nextInt();
    }

    public List expose() {
        return sharedList;
    }

    public boolean isMagic(int x) {
        return x == 42;
    }

    public void nested() {
        try {
            InputStream is = Files.newInputStream(Paths.get("missing"));
            try {
                is.read();
            } catch (IOException ex) {
            }
        } catch (IOException ex) {
        }
    }

    public Date legacyNow() {
        return new Date();
    }

    public Calendar legacyCalendar() {
        return Calendar.getInstance();
    }
}
