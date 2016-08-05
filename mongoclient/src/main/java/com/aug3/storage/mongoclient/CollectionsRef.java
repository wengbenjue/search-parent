package com.aug3.storage.mongoclient;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 表映射关系
 * 
 * @author jimmy.zhou
 *
 */
public class CollectionsRef {

	// ada
	public static final String	COLL_BASE_BLOCK_TRADE					= "ada.base_block_trade";
	public static final String	COLL_BASE_BOND							= "ada.base_bond";
	public static final String	COLL_BASE_BUSINESS_PERSPECTIVE			= "ada.base_business_perspective";
	public static final String	COLL_BASE_BUSINESS_REVIEW				= "ada.base_business_review";
	public static final String	COLL_BASE_CS_INVESTMENT					= "ada.base_cs_investment";
	public static final String	COLL_BASE_EARNINGS_PREANNOUNCEMENT		= "ada.base_earnings_preannouncement";
	public static final String	COLL_BASE_EMPLOYEE						= "ada.base_employee";
	public static final String	COLL_BASE_ENTERPRISE					= "ada.base_enterprise";
	public static final String	COLL_BASE_ENTERPRISE_ANNUITY			= "ada.base_enterprise_annuity";
	public static final String	COLL_BASE_EXECUTIVE						= "ada.base_executive";
	public static final String	COLL_BASE_EXECUTIVE_REGULATION			= "ada.base_executive_regulation";
	public static final String	COLL_BASE_EXECUTIVE_SHAREHOLDER			= "ada.base_executive_shareholder";
	public static final String	COLL_BASE_HOUSEHOLDS					= "ada.base_households";
	public static final String	COLL_BASE_INTERMEDIARY					= "ada.base_intermediary";
	public static final String	COLL_BASE_MARGIN_TRADING				= "ada.base_margin_trading";
	public static final String	COLL_BASE_ORG							= "ada.base_org";
	public static final String	COLL_BASE_ORG_POOL						= "ada.base_org_pool";
	public static final String	COLL_BASE_PEOPLE						= "ada.base_people";
	public static final String	COLL_BASE_PORTFOLIO						= "ada.base_portfolio";
	public static final String	COLL_BASE_REPURCHASE					= "ada.base_repurchase";
	public static final String	COLL_BASE_RESEARCHER					= "ada.base_researcher";
	public static final String	COLL_BASE_SECURITY_INTERMEDIARY			= "ada.base_security_intermediary";
	public static final String	COLL_BASE_SHARE_VARY					= "ada.base_share_vary";
	public static final String	COLL_BASE_SHAREHOLDER					= "ada.base_shareholder";
	public static final String	COLL_BASE_SHAREHOLDER_DEF				= "ada.base_shareholder_def";
	public static final String	COLL_BASE_STOCK							= "ada.base_stock";
	public static final String	COLL_CED_INDICATOR						= "ada.ced_indicator";
	public static final String	COLL_CED_INDICATOR_CALENDAR				= "ada.ced_indicator_calendar";
	public static final String	COLL_CED_INDICATOR_DATA					= "ada.ced_indicator_data";
	public static final String	COLL_COMPANY_SCREENER_CRITERIA			= "ada.company_screener_criteria";
	public static final String	COLL_COMPANY_SCREENER_STRATEGY			= "ada.company_screener_strategy";
	public static final String	COLL_CSF_DICT							= "ada.csf_dict";
	public static final String	COLL_DICT_ANNOUNCE_CATALOG				= "ada.dict_announce_catalog";
	public static final String	COLL_DICT_ANNOUNCE_CATALOG_HK			= "ada.dict_announce_catalog_hk";
	public static final String	COLL_DICT_ANNOUNCE_CUSTOMTYP			= "ada.dict_announce_customtyp";
	public static final String	COLL_DICT_ANNOUNCE_RULE					= "ada.dict_announce_rule";
	public static final String	COLL_DICT_CALENDAR						= "ada.dict_calendar";
	public static final String	COLL_DICT_CED_INDICATOR_TYPE			= "ada.dict_ced_indicator_type";
	public static final String	COLL_DICT_CED_SOURCE					= "ada.dict_ced_source";
	public static final String	COLL_DICT_COMMON						= "ada.dict_common";
	public static final String	COLL_DICT_CORPORATE_TYPE				= "ada.dict_corporate_type";
	public static final String	COLL_DICT_CURRENCY						= "ada.dict_currency";
	public static final String	COLL_DICT_EDUCATION_BACKGROUND			= "ada.dict_education_background";
	public static final String	COLL_DICT_ENTERPRISE					= "ada.dict_enterprise";
	public static final String	COLL_DICT_ENTERPRISE_REGISTER_TYPE		= "ada.dict_enterprise_register_type";
	public static final String	COLL_DICT_EXECUTIVE_POSITION			= "ada.dict_executive_position";
	public static final String	COLL_DICT_FIN_RPT_CATLOG				= "ada.dict_fin_rpt_catlog";
	public static final String	COLL_DICT_FREQ							= "ada.dict_freq";
	public static final String	COLL_DICT_INDEX							= "ada.dict_index";
	public static final String	COLL_DICT_INDEX_REASON					= "ada.dict_index_reason";
	public static final String	COLL_DICT_INDICATOR_TYPE				= "ada.dict_indicator_type";
	public static final String	COLL_DICT_INDUSTRY						= "ada.dict_industry";
	public static final String	COLL_DICT_INDUSTRY_GB					= "ada.dict_industry_gb";
	public static final String	COLL_DICT_INTERMEDIARY_TYPE				= "ada.dict_intermediary_type";
	public static final String	COLL_DICT_LISTING_STATUS				= "ada.dict_listing_status";
	public static final String	COLL_DICT_MAGNITUDE						= "ada.dict_magnitude";
	public static final String	COLL_DICT_MARKET						= "ada.dict_market";
	public static final String	COLL_DICT_NATURE						= "ada.dict_nature";
	public static final String	COLL_DICT_NEWS_CATALOG					= "ada.dict_news_catalog";
	public static final String	COLL_DICT_ORG_TYPE						= "ada.dict_org_type";
	public static final String	COLL_DICT_PRODUCT						= "ada.dict_product";
	public static final String	COLL_DICT_PRODUCT_CED					= "ada.dict_product_ced";
	public static final String	COLL_DICT_PROFESSIONAL_QUALIFICATION	= "ada.dict_professional_qualification";
	public static final String	COLL_DICT_PROFESSIONAL_TITLE			= "ada.dict_professional_title";
	public static final String	COLL_DICT_REGION						= "ada.dict_region";
	public static final String	COLL_DICT_SECURITY_TYPE					= "ada.dict_security_type";
	public static final String	COLL_DICT_SENSITIVE_WORD				= "ada.dict_sensitive_word";
	public static final String	COLL_DICT_SHAREHOLDER_TYPE				= "ada.dict_shareholder_type";
	public static final String	COLL_DICT_STD_TYPE						= "ada.dict_std_type";
	public static final String	COLL_DICT_UNITS							= "ada.dict_units";
	public static final String	COLL_DICT_YTD_TYPE						= "ada.dict_ytd_type";
	public static final String	COLL_ERROR_IDS							= "ada.error_ids";
	public static final String	COLL_FIN_COMPS_METRICS					= "ada.fin_comps_metrics";
	public static final String	COLL_FIN_RPT_CATLOG						= "ada.fin_rpt_catlog";
	public static final String	COLL_FIN_RPT_CATLOG_DEF					= "ada.fin_rpt_catlog_def";
	public static final String	COLL_FIN_RPT_CATLOG_DEFAULT				= "ada.fin_rpt_catlog_default";
	public static final String	COLL_FIN_SAM_STYLE						= "ada.fin_sam_style";
	public static final String	COLL_INDEX_CHANGE_HIS					= "ada.index_change_his";
	public static final String	COLL_INDEX_SPECIMEN_STOCK				= "ada.index_specimen_stock";
	public static final String	COLL_INDEX_VALUATION					= "ada.index_valuation";
	public static final String	COLL_NEWS								= "ada.news";
	public static final String	COLL_RESEARCH_USRCONFIG					= "ada.research_usrconfig";
	public static final String	COLL_RR_RATING							= "ada.rr_rating";
	public static final String	COLL_RR_RESEARCH_ORG					= "ada.rr_research_org";
	public static final String	COLL_RR_RESEARCHER						= "ada.rr_researcher";
	public static final String	COLL_RR_RPTTYPE							= "ada.rr_rpttype";
	public static final String	COLL_SEARCH_STATISTIC					= "ada.search_statistic";
	public static final String	COLL_USERREQUEST						= "ada.userrequest";
	public static final String	COLL_USERSEARCHCART						= "ada.usersearchcart";
	public static final String	COLL_WARRANTS_CBBC_INFO					= "ada.warrants_cbbc_info";
	public static final String	COLL_WARRANTS_CBBC_ISSUE				= "ada.warrants_cbbc_issue";
	public static final String	COLL_X_FUNCS							= "ada.x_funcs";
	public static final String	COLL_X_FUNCS_CATALOG					= "ada.x_funcs_catalog";
	public static final String	COLL_X_FUNCS_PARAMS						= "ada.x_funcs_params";
	public static final String	COLL_X_SECU_ADAPTOR						= "ada.x_secu_adaptor";

	// metrics
	public static final String	COLL_ANNOUNCE_RET						= "metrics.announce_ret";
	public static final String	COLL_ANNOUNCE_STAT						= "metrics.announce_stat";
	public static final String	COLL_CSF_INDEX							= "metrics.csf_index";
	public static final String	COLL_EQUITY_PRICE__US_HQ				= "metrics.equity_price__us_hq";
	public static final String	COLL_EQUITY_PRICE_HQ					= "metrics.equity_price_hq";
	public static final String	COLL_FIN_ANALYSIS_D_2000				= "metrics.fin_analysis_d_2000";
	public static final String	COLL_FIN_ANALYSIS_D_2002				= "metrics.fin_analysis_d_2002";
	public static final String	COLL_FIN_ANALYSIS_D_2004				= "metrics.fin_analysis_d_2004";
	public static final String	COLL_FIN_ANALYSIS_D_2005				= "metrics.fin_analysis_d_2005";
	public static final String	COLL_FIN_ANALYSIS_D_2006				= "metrics.fin_analysis_d_2006";
	public static final String	COLL_FIN_ANALYSIS_D_2007				= "metrics.fin_analysis_d_2007";
	public static final String	COLL_FIN_ANALYSIS_D_2008				= "metrics.fin_analysis_d_2008";
	public static final String	COLL_FIN_ANALYSIS_D_2009				= "metrics.fin_analysis_d_2009";
	public static final String	COLL_FIN_ANALYSIS_D_2010				= "metrics.fin_analysis_d_2010";
	public static final String	COLL_FIN_ANALYSIS_D_2011				= "metrics.fin_analysis_d_2011";
	public static final String	COLL_FIN_ANALYSIS_D_2012				= "metrics.fin_analysis_d_2012";
	public static final String	COLL_FIN_ANALYSIS_D_2013				= "metrics.fin_analysis_d_2013";
	public static final String	COLL_FIN_ANALYSIS_D_2014				= "metrics.fin_analysis_d_2014";
	public static final String	COLL_FIN_ANALYSIS_D_2015				= "metrics.fin_analysis_d_2015";
	public static final String	COLL_FIN_ANALYSIS_D_NEW					= "metrics.fin_analysis_d_new";
	public static final String	COLL_FIN_ANALYSIS_D_OLD					= "metrics.fin_analysis_d_old";
	public static final String	COLL_FIN_ANALYSIS_Q						= "metrics.fin_analysis_q";
	public static final String	COLL_FIN_SAM_ANALYSIS					= "metrics.fin_sam_analysis";
	public static final String	COLL_FIN_SAM_ANALYSIS_RT				= "metrics.fin_sam_analysis_rt";
	public static final String	COLL_PRICE_ANALYSE						= "metrics.price_analyse";
	public static final String	COLL_RR_RESEARCHER_ACCURACY				= "metrics.rr_researcher_accuracy";

	// fin
	public static final String	COLL_BASE_DIVIDEND						= "fin.base_Dividend";
	public static final String	COLL_FIN_ACC_NAT						= "fin.fin_acc_nat";
	public static final String	COLL_FIN_ACC_STD						= "fin.fin_acc_std";
	public static final String	COLL_FIN_CORP_NATURE					= "fin.fin_corp_nature";
	public static final String	COLL_FIN_HIGHLIGHT						= "fin.fin_highlight";
	public static final String	COLL_FIN_ITEM							= "fin.fin_item";
	public static final String	COLL_FIN_ITEM_DEF						= "fin.fin_item_def";
	public static final String	COLL_FIN_RPT							= "fin.fin_rpt";
	public static final String	COLL_FIN_RPT_ASREP						= "fin.fin_rpt_asrep";
	public static final String	COLL_FIN_RPT_ASREP_EXT_CUR				= "fin.fin_rpt_asrep_ext_cur";
	public static final String	COLL_FIN_RPT_ASREP_EXT_HYR				= "fin.fin_rpt_asrep_ext_hyr";
	public static final String	COLL_FIN_RPT_ASREP_EXT_LTM				= "fin.fin_rpt_asrep_ext_ltm";
	public static final String	COLL_FIN_RPT_ASREP_EXT_YTD				= "fin.fin_rpt_asrep_ext_ytd";
	public static final String	COLL_FIN_RPT_AUDIT						= "fin.fin_rpt_audit";
	public static final String	COLL_FIN_RPT_BREAKDOWN					= "fin.fin_rpt_breakdown";
	public static final String	COLL_FIN_RPT_STYLE						= "fin.fin_rpt_style";
	public static final String	COLL_FIN_RPT_STYLE_COMMON				= "fin.fin_rpt_style_common";
	public static final String	COLL_FIN_RPT_TPL_CUR					= "fin.fin_rpt_tpl_cur";
	public static final String	COLL_FIN_RPT_TPL_HYR					= "fin.fin_rpt_tpl_hyr";
	public static final String	COLL_FIN_RPT_TPL_LTM					= "fin.fin_rpt_tpl_ltm";
	public static final String	COLL_FIN_RPT_TPL_YTD					= "fin.fin_rpt_tpl_ytd";

	// fund
	public static final String	COLL_BASE_FUND							= "fund.base_fund";
    public static final String	COLL_BASE_FUND_RAISE 					= "fund.base_fund_raise";
    public static final String	COLL_BASE_FUND_SHARE 					= "fund.base_fund_share";
    public static final String	COLL_BASE_FUND_MANAGER 					= "fund.base_fund_manager";
    public static final String	COLL_FUND_NET_VALUE 					= "fund.fund_net_value";
    public static final String	COLL_FUND_EARNING_DAILY 				= "fund.fund_earning_daily";
    public static final String	COLL_FUND_DIVIDEND 						= "fund.fund_dividend";
    public static final String	COLL_FUND_BOND_PORTFOLIO 				= "fund.fund_bond_portfolio";
    public static final String	COLL_FUND_TOP_FIVE_BOND 				= "fund.fund_top_five_bond";
    public static final String	COLL_FUND_ASSET_COMBINATION 			= "fund.fund_asset_combination";
    public static final String	COLL_FUND_TOP_TEN_STOCK 				= "fund.fund_top_ten_stock";
    public static final String	COLL_FUND_STOCK_DETAIL 					= "fund.fund_stock_detail";
    public static final String	COLL_BASE_ETF_COMPONENT 				= "fund.base_etf_component";
    public static final String	COLL_BASE_EFT_PURCHASE_REDEMPTION 		= "fund.base_eft_purchase_redemption";

    // invest
	public static final String	COLL_COMBINATION_BASE					= "invest.combination_base";
	public static final String	COLL_COMBINATION_HIS					= "invest.combination_his";
	public static final String	COLL_COMBINATION_NET_VAL				= "invest.combination_net_val";
	public static final String	COLL_COMPANY_EVALUATION					= "invest.company_evaluation";
	public static final String	COLL_FIN_CSF_ANALYSIS					= "invest.fin_csf_analysis";
	public static final String	COLL_FIN_CSF_ANALYSIS_RT				= "invest.fin_csf_analysis_rt";
	public static final String	COLL_MOBILE_USER_MESSAGE				= "invest.mobile_user_message";
	public static final String	COLL_STRATEGY_BASE						= "invest.strategy_base";
	public static final String	COLL_STRATEGY_DETAIL					= "invest.strategy_detail";

	// news
	public static final String	COLL_ANNOUNCEMENT						= "news.announcement";
	public static final String	COLL_ANNOUNCEMENT_HK					= "news.announcement_hk";
	public static final String	COLL_ANNOUNCEMENT_HK_CHZ				= "news.announcement_hk_chz";
	public static final String	COLL_ANNOUNCEMENT_US					= "news.announcement_us";
	public static final String	COLL_CRAWL_HOTSPOT						= "news.crawl_hotspot";
	public static final String	COLL_CRAWL_KEYWORD						= "news.crawl_keyword";
	public static final String	COLL_KEYWORD_DICT						= "news.keyword_dict";
	public static final String	COLL_KEYWORD_DICT_ORIG					= "news.keyword_dict_orig";
	public static final String	COLL_RESEARCH_REPORT					= "news.research_report";
	public static final String	COLL_RESEARCH_REPORT_DEF				= "news.research_report_def";
	public static final String	COLL_RESEARCH_REPORT_NEW				= "news.research_report_new";
	
	// news2
	public static final String	COLL_HOTNEWS_ANALYSE					= "news2.hotnews_analyse";
	public static final String	COLL_TOPNEWS_ANALYSE					= "news2.topnews_analyse";

	// opt
	public static final String	COLL_BASE_SUPPLY_CHAIN					= "opt.base_supply_chain";
	public static final String	COLL_DICT_PRODUCT_DEF					= "opt.dict_product_def";
	public static final String	COLL_DICT_PRODUCT_RS					= "opt.dict_product_rs";
	public static final String	COLL_FIN_SAM_PRODUCT					= "opt.fin_sam_product";
	public static final String	COLL_FIN_SAM_REGION						= "opt.fin_sam_region";
	public static final String	COLL_MAIN_INDUSTRY						= "opt.main_industry";
	public static final String	COLL_OPT_METRICS						= "opt.opt_metrics";
	public static final String	COLL_SAM_REGION_DEF						= "opt.sam_region_def";
	public static final String	COLL_SAM_REGION_RS						= "opt.sam_region_rs";
	public static final String	COLL_SUPPLY_CHAIN_DRIVER				= "opt.supply_chain_driver";
	public static final String	COLL_SUPPLY_CHAIN_RELATION				= "opt.supply_chain_relation";

	// shukuyun
	public static final String	COLL_EVAL_COMM_COMP_RPT					= "shukuyun.eval_comm_comp_rpt";
	public static final String	COLL_EVAL_COMM_IND_RPT					= "shukuyun.eval_comm_ind_rpt";
	public static final String	COLL_EVAL_COMPANY_INFO					= "shukuyun.eval_company_info";
	public static final String	COLL_EVAL_FIN_ABSOLUTE					= "shukuyun.eval_fin_absolute";
	public static final String	COLL_EVAL_FIN_ANALYSIS					= "shukuyun.eval_fin_analysis";
	public static final String	COLL_EVAL_FIN_BS						= "shukuyun.eval_fin_bs";
	public static final String	COLL_EVAL_FIN_DUPONT					= "shukuyun.eval_fin_dupont";
	public static final String	COLL_EVAL_FIN_FUND						= "shukuyun.eval_fin_fund";
	public static final String	COLL_EVAL_FIN_RPT_HYR					= "shukuyun.eval_fin_rpt_hyr";
	public static final String	COLL_EVAL_MODEL_STAT					= "shukuyun.eval_model_stat";
	public static final String	COLL_EVAL_PUB_INV_RPT					= "shukuyun.eval_pub_inv_rpt";
	public static final String	COLL_EVAL_RPT_APPLY						= "shukuyun.eval_rpt_apply";
	public static final String	COLL_EVAL_RPT_FOCUS						= "shukuyun.eval_rpt_focus";
	public static final String	COLL_EVAL_RPT_HOT_AREA					= "shukuyun.eval_rpt_hot_area";
	public static final String	COLL_EVAL_RPT_HOT_PRODUCT				= "shukuyun.eval_rpt_hot_product";
	public static final String	COLL_EVAL_USER_INFO						= "shukuyun.eval_user_info";
	public static final String	COLL_EVAL_USER_MESSAGE					= "shukuyun.eval_user_message";
	public static final String	COLL_PEVC_DICT							= "shukuyun.pevc_dict";
	public static final String	COLL_PEVC_FIN							= "shukuyun.pevc_fin";
	public static final String	COLL_PEVC_FUND							= "shukuyun.pevc_fund";
	public static final String	COLL_PEVC_FUND_LP						= "shukuyun.pevc_fund_lp";
	public static final String	COLL_PEVC_MANAGEMENT					= "shukuyun.pevc_management";
	public static final String	COLL_PEVC_ORG							= "shukuyun.pevc_org";
	public static final String	COLL_PEVC_PEOPLE						= "shukuyun.pevc_people";
	public static final String	COLL_PEVC_PROJECT						= "shukuyun.pevc_project";
	public static final String	COLL_PEVC_PROJECT_DETAIL				= "shukuyun.pevc_project_detail";
	public static final String	COLL_PEVC_PROJECT_SHAREHOLDER			= "shukuyun.pevc_project_shareholder";
	public static final String	COLL_PEVC_STATISTIC						= "shukuyun.pevc_statistic";
	public static final String	COLL_PEVC_STATISTIC_TEMP				= "shukuyun.pevc_statistic_temp";
	
	// vsto
	public static final String COLL_FIN_FP_TEMP 						= "vsto.fin_fp_temp";
	public static final String COLL_FIN_ITEM_CALC 						= "vsto.fin_item_calc";
	public static final String COLL_FIN_ITEM_CALC_H 					= "vsto.fin_item_calc_h";
	public static final String COLL_FIN_ITEM_CALC_IB 					= "vsto.fin_item_calc_ib";
	public static final String COLL_FIN_ITEM_CALC_TW 					= "vsto.fin_item_calc_tw";
	public static final String COLL_IDS 								= "vsto.ids";
	public static final String COLL_INDEX_CHANGE_MESSAGE 				= "vsto.index_change_message";
	public static final String COLL_INDEX_EX_CHANGE 					= "vsto.index_ex_change";
	public static final String COLL_INDEX_SPECIMEN_STOCK_CALC 			= "vsto.index_specimen_stock_calc";
	public static final String COLL_OPT_METRICS_RPT 					= "vsto.opt_metrics_rpt";
	public static final String COLL_OPT_TASK 							= "vsto.opt_task";
	public static final String COLL_OPT_TASK_STAT_HISTORY 				= "vsto.opt_task_stat_history";
	public static final String COLL_QSM_TASK 							= "vsto.qsm_task";

	
	
	public static Map<String, String> coll2dbMap = new HashMap<String, String>();
	static {
		Field[] fields = CollectionsRef.class.getFields();
		for(Field f : fields) {
			try{
				String fn = f.getName();
				if(!fn.startsWith("COLL_")) continue;
				String val = (String)f.get(null);
				String dbName = val.split("\\.")[0];
				String collName = val.split("\\.")[1];
				coll2dbMap.put(collName, dbName);
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
}
