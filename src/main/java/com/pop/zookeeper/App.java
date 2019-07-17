package com.pop.zookeeper;

import java.util.HashSet;
import java.util.Set;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {

        Set<Short> set = new HashSet<>();
        for (short i = 0; i <5 ; i++) {
            set.add(i);
//            set.add(i-1);
            boolean flag=set.remove(i-1);
            System.out.print(i-1+" ");
        }
    }
}
