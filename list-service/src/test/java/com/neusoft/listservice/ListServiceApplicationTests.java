package com.neusoft.listservice;

import com.alibaba.dubbo.config.annotation.Reference;
import com.neusoft.interfaces.SkuService;
import com.neusoft.javabean.po.SkuAttrValue;
import com.neusoft.javabean.po.SkuInfo;
import com.neusoft.javabean.po.SkuLsInfo;
import io.searchbox.client.JestClient;
import io.searchbox.core.Delete;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.beanutils.BeanUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ListServiceApplicationTests {

    @Autowired
    JestClient jestClient;

    @Reference
    SkuService skuService;

    public static String getMyDsl(){

        SearchSourceBuilder dsl = new SearchSourceBuilder();

        //创建一个先过滤后搜索的query
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        //过滤语句
        TermQueryBuilder t1 = new TermQueryBuilder("catalog3Id","1");
        boolQueryBuilder.filter(t1);
        TermQueryBuilder t2 = new TermQueryBuilder("skuAttrValueList.valueId","142");
        boolQueryBuilder.filter(t2);
        TermQueryBuilder t3 = new TermQueryBuilder("skuAttrValueList.valueId","153");
        boolQueryBuilder.filter(t3);

        //并集过滤
        String[] strs = new String[2];
        strs[0]="85";
        strs[1]="112";
        TermsQueryBuilder termsQueryBuilder = new TermsQueryBuilder("skuAttrValueList.valueId",strs);
        boolQueryBuilder.filter(termsQueryBuilder);

        //搜索语句
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName","金色");
        boolQueryBuilder.must(matchQueryBuilder);
        MatchQueryBuilder matchQueryBuilder1 = new MatchQueryBuilder("skuDesc","双卡双待");
        boolQueryBuilder.must(matchQueryBuilder1);

        dsl.query(boolQueryBuilder);
        dsl.from(0);
        dsl.size(20);
        return dsl.toString();
    }


    @Test
    public void search(){
        List<SkuLsInfo> skuLsInfos = new ArrayList<>();

        //如何查询es中的数据
        Search search = new Search.Builder(getMyDsl()).addIndex("guli2019").addType("SkuLsInfo").build();

        try {
            SearchResult execute = jestClient.execute(search);
            List<SearchResult.Hit<SkuLsInfo, Void>> hits = execute.getHits(SkuLsInfo.class);
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                SkuLsInfo source = hit.source;
                skuLsInfos.add(source);
            }
            System.out.println(skuLsInfos.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void deleteEs(){
        Delete build = new Delete.Builder("55").index("neusoft0328").type("SkuLsInfo").build();
        try {
            jestClient.execute(build);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void contextLoads() {
        // 查询mysql中的sku信息
        List<SkuInfo> skuInfoList = skuService.getSkuListByCatalog3Id(61l);

        // 转化es中的sku信息
        List<SkuLsInfo> skuLsInfos = new ArrayList<>();

        for (SkuInfo skuInfo : skuInfoList) {
            SkuLsInfo skuLsInfo = new SkuLsInfo();

            try {
                BeanUtils.copyProperties(skuLsInfo,skuInfo);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            skuLsInfos.add(skuLsInfo);
        }

        // 导入到es中
        for (SkuLsInfo skuLsInfo : skuLsInfos) {
            String id = skuLsInfo.getId();

            Index build = new Index.Builder(skuLsInfo).index("guli2019").type("SkuLsInfo").id(id).build();

            System.out.println(build.toString());
            try {
                jestClient.execute(build);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     *     将sql中的数据存放到elastic中
     */
    @Test
    public void toElastic11() {


    }
}
