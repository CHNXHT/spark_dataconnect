package com.idata.plugin.cf;

import com.idata.DataSource;
import com.idata.model.cf.T_SJKJ_RMTJ_AJBL;
import com.idata.model.hhm.t_mediation_case;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import scala.Function1;
import java.io.Serializable;
import static com.idata.ReadData.getRawDF;
import static com.idata.utils.connectionUtil.hhm_mysqlProperties;

/**
 * @description: some desc
 * @author: xiehaotian
 * @date: 2023/7/10 16:29
 */
public class ajblCaseSync {
    public static void main(String[] args) {

        SparkSession spark = SparkSession.builder()
                .appName("ajblCaseSync")
//                .master("local[2]")
                .getOrCreate();

        /*
          dataSourceName包括如下
          1、JMLT
          2、CF
          3、HHM
         */
        String dataSourceName = "CF";//args[0];
        String tableName = "T_SJKJ_RMTJ_AJBL";//args[1];
        String targetTableName = "t_mediation_case";

        //获取来源表数据
        Dataset<Row> rawDF = getRawDF(spark, tableName, dataSourceName);
        Dataset<Row> rowDataset = rawDF.withColumn("SAJE", rawDF.col("SAJE").cast(DataTypes.LongType));;
        //定义数据源对象
        Dataset<T_SJKJ_RMTJ_AJBL> rowDF = rowDataset.as(Encoders.bean(T_SJKJ_RMTJ_AJBL.class));
        rowDF.show();

        //转化为目标表结构
        Dataset<t_mediation_case> tcDF = rowDF
                .map(new ajblCaseSync.ConvertToTMediationCase(), Encoders.bean(t_mediation_case.class));

        tcDF
                .write()
                .mode(SaveMode.Append)
                .jdbc(DataSource.HHM.getUrl(), targetTableName, hhm_mysqlProperties());
    }

    public static class ConvertToTMediationCase implements Function1<T_SJKJ_RMTJ_AJBL, t_mediation_case>, Serializable {
        @Override
        public t_mediation_case apply(T_SJKJ_RMTJ_AJBL ajbl) {
            t_mediation_case tMediationCase = new t_mediation_case();
            tMediationCase.setResource_id(ajbl.getBH());
            //创建时间
            tMediationCase.setCreate_time(ajbl.getSLRQ());
            //修改时间
//            tMediationCase.setUpdate_time(DateUtils.strToTsSFM(ajbl.getGXSJ()));
            //纠纷编号
            tMediationCase.setCase_num(ajbl.getAJBH());
            //纠纷描述
            tMediationCase.setCase_description(ajbl.getJFJJ());
            //纠纷诉求
//            tMediationCase.setRequest("-");
            //调解方式
            tMediationCase.setMethod(2);
            //证据材料描述
//            tMediationCase.setEvidence_description("-");
            //纠纷类型
            if(ajbl.getJFLX() != null){
                tMediationCase.setCase_type(ajbl.getJFLX());
            }
            //纠纷发生地
            tMediationCase.setPlace_detail(ajbl.getXZQH());
            //纠纷发生日期
            tMediationCase.setOccurrence_time(ajbl.getFSRQ());
            //创建人ID
            tMediationCase.setCreate_user_id(10101L);
            //创建人姓名
            tMediationCase.setCreate_user_name(ajbl.getTJY());
            //文书状态
            tMediationCase.setDoc_status(0);
            //调解结果
//            if (StringUtils.isBlank(ajbl.getTJZT())) {
//                tMediationCase.setResult(1);  //todo 检查确认
//            } else {
//                try {
//                    tMediationCase.setResult(Integer.parseInt(ajbl.getTJZT()));
//                } catch (NumberFormatException e) {
//                    // 处理转换异常，例如设定一个默认值或者抛出自定义异常
//                }
//            }

            //纠纷状态 先处理办理状态 再处理调整状态
//            String blzt = ajbl.getBLZT();
//            String sjzt = ajbl.getSJZT();
//            if(StringUtils.isAllBlank(blzt)){
//                if(0 == Integer.parseInt(blzt)){
//                    tMediationCase.setStatus(1);
//                } else if(2 == Integer.parseInt(blzt)){
//                    tMediationCase.setStatus(4);
//                }else{
//                    if(StringUtils.isAllBlank(sjzt)){
//                        if(1 == Integer.parseInt(sjzt) || 2 == Integer.parseInt(sjzt) || 3 == Integer.parseInt(sjzt)){
//                            tMediationCase.setStatus(2);
//                        }else if(4 == Integer.parseInt(sjzt)){
//                            tMediationCase.setStatus(7);
//                        }else if(5 == Integer.parseInt(sjzt) || 6 == Integer.parseInt(sjzt) || 7 == Integer.parseInt(sjzt) || 8 == Integer.parseInt(sjzt)){
//                            tMediationCase.setStatus(4);
//                        }
//                    }
//                }
//            }
            //纠纷来源 1为警民联调
            tMediationCase.setCase_source(2);

            // 设置其他属性
            return tMediationCase;
        }
    }
}
