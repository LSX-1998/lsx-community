package com.lsx.community.config;


//import com.lsx.community.quartz.AlphaJob ;
import com.lsx.community.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

/*
  配置->数据库 -> 调用
 */
@Configuration
public class QuartzConfig {

    //FactoryBean 可以简化Bean的实例过程:
    //1.通过FactoryBean封装Bean的实例化过程
    //2.将FactoryBean装配到Spring容器中
    //3.将FactoryBean注入到其他容器中
    //4.该Bean得到的是FactoryBean所管理的对象实例

    //配置JobDetail
    //@Bean
   /* public JobDetailFactoryBean alphaJobDetail(){
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean() ;
        factoryBean.setJobClass(AlphaJob.class);
        factoryBean.setBeanName("alphaJob");
        factoryBean.setGroup("alphaJobGroup");
        factoryBean.setDurability(true);
        return  factoryBean ;
    }


    //配置Trigger
   // @Bean
    public SimpleTriggerFactoryBean alphaTrigger(JobDetail alphaJobDetail){

        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean() ;
        factoryBean.setJobDetail(alphaJobDetail);
        factoryBean.setBeanName("alphaTrigger");
        factoryBean.setGroup("alphaTriggerGroup");
        factoryBean.setRepeatInterval(3000);
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean ;
    }*/


    //刷新帖子分数任务
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail(){
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean() ;
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        factoryBean.setBeanName("postScoreRefreshJob");
        factoryBean.setGroup("communityJobGroup");
        factoryBean.setDurability(true);
        factoryBean.setRequestsRecovery(true);
        return  factoryBean ;
    }


    //配置Trigger
     @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail){

        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean() ;
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        factoryBean.setBeanName("postScoreRefreshTrigger");
        factoryBean.setGroup("CommunityTriggerGroup");
        factoryBean.setRepeatInterval(1000*60*5);
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean ;
    }
}
