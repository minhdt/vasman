package com.crm.provisioning.impl;

import java.sql.Connection;
import java.util.ArrayList;

import com.crm.kernel.message.Constants;
import com.crm.kernel.sql.Database;
import com.crm.product.cache.ProductEntry;
import com.crm.product.cache.ProductFactory;
import com.crm.product.cache.ProductMessage;
import com.crm.provisioning.cache.ProvisioningCommand;
import com.crm.provisioning.message.CommandMessage;
import com.crm.provisioning.message.VNMMessage;
import com.crm.provisioning.thread.CommandInstance;
import com.crm.provisioning.util.CommandUtil;
import com.crm.provisioning.util.ResponseUtil;
import com.crm.subscriber.bean.SubscriberProduct;
import com.crm.subscriber.impl.SubscriberProductImpl;

public class SupportKeywordCommandImpl extends CommandImpl
{
	public VNMMessage checkServiceStatus(CommandInstance instance,
			ProvisioningCommand provisioningCommand, CommandMessage request)
			throws Exception
	{
		VNMMessage result = CommandUtil.createVNMMessage(request);
		
		try
		{
			ProductEntry product = ProductFactory.getCache().getProduct(request.getProductId());
			String listProductId = product.getParameters().getString("ListProductId", "");
	
			ArrayList<SubscriberProduct> arrProduct = SubscriberProductImpl.getActive(result.getIsdn(), listProductId);
	
			if (arrProduct.isEmpty())
			{
				result.setCause(Constants.ERROR_SERVICE_LIST_NOT_FOUND);
				return result;
			}
	
			ProductMessage productMessage = null;
	
			ProductEntry productCache = null;
	
			for (SubscriberProduct subProduct : arrProduct)
			{
				productCache = ProductFactory.getCache().getProduct(subProduct.getProductId());
	
				productMessage = product.getProductMessage(result.getActionType(), result.getCampaignId(), result.getLanguageId(),
						result.getChannel(), productCache.getAlias() + ".active");

				if (productMessage != null)
				{
					String content = productMessage.getContent();
					
					result.setResponseValue(ResponseUtil.SERVICE_START_DATE, subProduct.getRegisterDate());
					result.setResponseValue(ResponseUtil.SERVICE_PRICE, productCache.getPrice());
					result.setResponseValue(ResponseUtil.SERVICE_AMOUNT, productCache.getParameters().getLong(product.getAlias() + ".amount", 0));

					CommandUtil.sendSMS(instance, result, content);
					Thread.sleep(1000);
				}
			}
		}
		catch (Exception e)
		{
			processError(instance, provisioningCommand, request, e);
		}
		
		return result;
	}
	
	public VNMMessage checkDataStatus(CommandInstance instance, ProvisioningCommand provisioningCommand, CommandMessage request) throws Exception{

		VNMMessage result = CommandUtil.createVNMMessage(request);
		try
		{
			ProductEntry product = ProductFactory.getCache().getProduct(request.getProductId());
			String listDataPackageId = product.getParameters().getString("ListDataPackageId", "");
	
			ArrayList<SubscriberProduct> arrProduct = SubscriberProductImpl.getActive(result.getIsdn(), listDataPackageId);
	
			if (arrProduct.isEmpty())
			{
				result.setCause(Constants.ERROR_PACKAGE_LIST_NOT_FOUND);
				return result;
			}
	
			ProductMessage productMessage = null;
	
			ProductEntry productCache = null;
	
			for (SubscriberProduct subProduct : arrProduct)
			{
				productCache = ProductFactory.getCache().getProduct(subProduct.getProductId());
	
				productMessage = product.getProductMessage(result.getActionType(), result.getCampaignId(), result.getLanguageId(),
						result.getChannel(), productCache.getAlias() + ".active");

				if (productMessage != null)
				{
					String content = productMessage.getContent();
					
					result.setResponseValue(ResponseUtil.SERVICE_START_DATE, subProduct.getRegisterDate());
					result.setResponseValue(ResponseUtil.SERVICE_PRICE, productCache.getPrice());
					result.setResponseValue(ResponseUtil.SERVICE_AMOUNT, productCache.getParameters().getLong(product.getAlias() + ".amount", 0));

					CommandUtil.sendSMS(instance, result, content);
					Thread.sleep(1000);
				}
			}
		}
		catch (Exception e)
		{
			processError(instance, provisioningCommand, request, e);
		}
		
		return result;
	}
	
	@SuppressWarnings("null")
	public VNMMessage cancelMultiDataPackage (CommandInstance instance, ProvisioningCommand provisioningCommand, CommandMessage request) throws Exception{
		VNMMessage result = CommandUtil.createVNMMessage(request);

		ProductEntry product = ProductFactory.getCache().getProduct(request.getProductId());
		String listDataPackageId = product.getParameters().getString("ListDataPackageId", "");
		Connection connection = null;
		
		ArrayList<SubscriberProduct> arrProduct = SubscriberProductImpl.getActive(result.getIsdn(), listDataPackageId);

		if (arrProduct.isEmpty()) {
			result.setCause(Constants.ERROR_PACKAGE_LIST_NOT_FOUND);
			return result;
		}
		String listActivePackage = arrListToString(arrProduct);
		connection = Database.getConnection();

		SubscriberProductImpl.unregisterMulti(connection, result.getIsdn(), listActivePackage);

		// Check Success UnregisterMulti DATA
		ArrayList<SubscriberProduct> arrProductErrorCancel = SubscriberProductImpl.getActive(result.getIsdn(), listDataPackageId);
		
		if(arrProductErrorCancel.isEmpty()){
			result.setCause("cancel.all." + Constants.SUCCESS);
			return result;
		}
		ProductMessage productMessage = null;
		ArrayList<SubscriberProduct> arrSuccessCancel = null;
		
		for(SubscriberProduct subArrProductErrorCancel : arrProductErrorCancel){
			for(SubscriberProduct subArrProduct : arrProduct){
				if(!subArrProduct.equals(subArrProductErrorCancel)){
					//arrSuccessCancel.add(arrProduct);
				}
			}	
		}
		productMessage = product.getProductMessage(result.getActionType(), result.getCampaignId(), result.getLanguageId(), result.getChannel(), "some.active");
		
		result.setResponseValue(ResponseUtil.SUCCESS_PRODUCT, arrSuccessCancel.toString());
		result.setResponseValue(ResponseUtil.FAILED_PRODUCT, arrProductErrorCancel.toString());

		CommandUtil.sendSMS(instance, result, productMessage.getContent());
		
		return result;
	}
	
	public static String arrListToString(ArrayList<SubscriberProduct> arrList){
		
		return arrList.toString().replaceAll("\\[|\\]", "");
		
	}
	
}
