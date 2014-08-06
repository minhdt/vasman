package com.crm.subscriber.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.crm.kernel.sql.Database;

public class DataPackageImpl
{
	public static boolean confirmUnRegister(long subscriberId, int subscriberStatus)
			throws Exception
	{
		boolean success = false;

		Connection connection = null;
		PreparedStatement stmtProduct = null;

		try
		{
			String sql = "Update SubscriberProduct Set status = ?, modifiedDate = sysdate, lastRunDate = sysdate Where subProductId = ? ";

			connection = Database.getConnection();
			stmtProduct = connection.prepareStatement(sql);
			
			stmtProduct.setInt(1, subscriberStatus);
			stmtProduct.setLong(2, subscriberId);

			stmtProduct.setQueryTimeout(10);

			stmtProduct.execute();
			
			if (stmtProduct.getUpdateCount() > 0)
			{
				success = true;
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			Database.closeObject(stmtProduct);
			Database.closeObject(connection);
		}

		return success;
	}
	
	public static boolean removeConfirm(long subscriberId, int subscriberStatus)
			throws Exception
	{
		boolean success = false;

		Connection connection = null;
		PreparedStatement stmtProduct = null;

		try
		{
			String strSQL = "Update SubscriberProduct Set status = ?, modifiedDate = sysdate, lastRunDate = sysdate Where subProductId = ? ";
			
			connection = Database.getConnection();
			stmtProduct = connection.prepareStatement(strSQL);

			stmtProduct.setInt(1, subscriberStatus);
			stmtProduct.setLong(2, subscriberId);
			
			stmtProduct.setQueryTimeout(10);

			stmtProduct.execute();
			
			if (stmtProduct.getUpdateCount() > 0)
			{
				success = true;
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			Database.closeObject(stmtProduct);
			Database.closeObject(connection);
		}
		
		return success;
	}
	
	public static boolean isConfirmRegister(long subscriberId, int subscriberStatus)
			throws Exception
	{
		boolean confirmed = false;

		Connection connection = null;
		PreparedStatement stmtProduct = null;
		ResultSet rsProduct = null;

		try
		{
			String strSQL = "Select * from SubscriberProduct where subproductid = ? and status= ?";
			
			connection = Database.getConnection();
			stmtProduct = connection.prepareStatement(strSQL);
			
			stmtProduct.setInt(1, subscriberStatus);
			stmtProduct.setLong(2, subscriberId);
			

			rsProduct = stmtProduct.executeQuery();
			if (rsProduct.next())
			{
				confirmed = true;
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			Database.closeObject(rsProduct);
			Database.closeObject(stmtProduct);
			Database.closeObject(connection);
		}

		return confirmed;
	}
}
