package com.tdull.db.ar;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 2020/2/14 17:24
 *
 * @author huangtao
 */
public class WhereTest {
    @Test
    public void parseTerm(){
        // ((a>8) AND (b<7)) AND ((c=3) OR (d=1))
        Where w = new Where();
        w.gt("a","8");
        w.lt("b","7");

        Where wc = new Where();
        wc.eq("c",3);
        Where wd = new Where();
        wd.eq("d",1);
        WhereTerm t = WhereTool.or(wc,wd);
        WhereTerm t2 = WhereTool.and(t,w);
        WhereData wx = WhereTool.parseTerm(t2);
        System.out.println(wx.getSql());
        System.out.println(wx.getData());
    }
}