package com.crm.provisioning.thread;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.crm.kernel.queue.QueueFactory;
import com.crm.kernel.sql.Database;
import com.crm.provisioning.message.CommandMessage;
import com.crm.thread.util.ThreadUtil;
import com.crm.util.StringUtil;
import com.fss.thread.ParameterType;
import com.fss.util.AppException;

public class LowBalanceAlertThread extends ProvisioningThread
{
	@SuppressWarnings("rawtypes")
	protected Vector vtAlert = new Vector();
	
	protected Connection connection = null;
	protected PreparedStatement _stmtQueue = null;
	protected ResultSet rsQueue = null;

	protected String _sqlCommand = "";
	protected int _restTime = 5;
	
	protected long enquireInterval = 900000;
	
	public ConcurrentHashMap<Long, Date>	indexes				= new ConcurrentHashMap<Long, Date>();

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getParameterDefinition()
	{
		Vector vtReturn = new Vector();

		Vector vtValue = new Vector();
		vtValue.addElement(createParameterDefinition("ProductId", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Context to mapping class", "0"));

		vtValue.addElement(createParameterDefinition("Limitation", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Context to mapping class", "1"));

		vtValue.addElement(createParameterDefinition("Balance", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Context to mapping class", "2"));

		vtValue.addElement(createParameterDefinition("ServiceAddress", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Context to mapping class", "3"));

		vtValue.addElement(createParameterDefinition("TimePerData", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Context to mapping class", "4"));

		vtValue.addElement(createParameterDefinition("SMSContent", "",
				ParameterType.PARAM_TEXTBOX_MAX, "",
				"Context to mapping class", "5"));

		vtReturn.addElement(createParameterDefinition("AlertConfig", "",
				ParameterType.PARAM_TABLE, vtValue, "Alert Config"));
		
		vtReturn.addElement(createParameterDefinition("SQLCommand", "",
				ParameterType.PARAM_TEXTBOX_MAX, "100"));
		vtReturn.addElement(createParameterDefinition("RestTime", "",
				ParameterType.PARAM_TEXTBOX_MAX, "100"));
		vtReturn.addElement(ThreadUtil.createLongParameter("EnquireInterval", ""));
		
		vtReturn.addAll(super.getParameterDefinition());

		return vtReturn;
	}

	// //////////////////////////////////////////////////////
	// Override
	// //////////////////////////////////////////////////////
	@SuppressWarnings("rawtypes")
	public void fillParameter() throws AppException
	{
		try
		{
			super.fillParameter();
			vtAlert = new Vector();
			Object obj = getParameter("AlertConfig");
			if (obj != null && (obj instanceof Vector))
			{
				vtAlert = (Vector) ((Vector) obj).clone();
			}
			
			setSQLCommand(loadMandatory("SQLCommand"));
			setRestTime(loadInteger("RestTime"));
			setEnquireInterval(loadLong("EnquireInterval"));
		}
		catch (AppException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void beforeProcessSession() throws Exception
	{
		super.beforeProcessSession();

		try
		{
			QueueFactory.getLocalQueue(queueLocalName).empty();
			indexes.clear();

			connection = Database.getConnection();
			
			String strSQL = getSQLCommand();
			_stmtQueue = connection.prepareStatement(strSQL);
		}
		catch (Exception e)
		{
			throw e;
		}
	}
	
	public void afterProcessSession() throws Exception
	{
		try
		{
			QueueFactory.getLocalQueue(queueLocalName).empty();
			indexes.clear();
			
			Database.closeObject(_stmtQueue);
			Database.closeObject(connection);
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			super.afterProcessSession();
		}
	}
	
	public CommandMessage pushMessage() throws Exception
	{
		CommandMessage request = new CommandMessage();

		request.setRequestTime(new Date());
		request.setUserName("system");
		request.setChannel("SMS");
		request.setSubProductId(rsQueue.getLong("subproductid"));
		request.setProductId(rsQueue.getLong("productid"));
		request.setServiceAddress("LBA");
		request.setIsdn(rsQueue.getString("isdn"));
		request.setKeyword("LowBalanceAlert");
		request.getParameters().setProperty("SubscriberStatus", StringUtil.valueOf(rsQueue.getInt("status")));

		return request;
	}
	
	public void doProcessSession() throws Exception
	{
		try
		{
			rsQueue = _stmtQueue.executeQuery();
	
			while (isAvailable() && rsQueue.next())
			{
				CommandMessage request = pushMessage();

				long requestId = rsQueue.getLong("subproductid");

				long now = System.currentTimeMillis();
				Date requestDate = indexes.get(requestId);
				if (requestDate == null
						|| (requestDate != null && (now - requestDate.getTime()) > 60000))
				{
					QueueFactory.attachLocal(queueLocalName, request);

					indexes.put(requestId, new Date());
				}
				else
				{
					logMonitor("SubProductId " + requestId + " is loaded");
				}

				Thread.sleep(getRestTime());
			}
		}
		catch (Exception e)
		{
			logMonitor(e);
			
//			sendInstanceAlarm(e, Constants.ERROR);
		}
	}

	@SuppressWarnings("rawtypes")
	public String getSMSContent(long productid)
	{
		String content = "";
		for (int i = 0; i < vtAlert.size(); i++)
		{
			Vector vt = (Vector) vtAlert.elementAt(i);

			if (Long.parseLong(vt.elementAt(0).toString()) == productid)
			{
				content = vt.elementAt(5).toString();
				break;
			}
		}
		return content;
	}

	@SuppressWarnings("rawtypes")
	public int getDataLimitation(long productid)
	{
		int dataLimitation = 0;
		for (int i = 0; i < vtAlert.size(); i++)
		{
			try
			{
				Vector vt = (Vector) vtAlert.elementAt(i);

				if (Long.parseLong(vt.elementAt(0).toString()) == productid)
				{
					dataLimitation = Integer.parseInt(vt.elementAt(1)
							.toString());
					break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return dataLimitation;
	}

	@SuppressWarnings("rawtypes")
	public int getTimePerData(long productid)
	{
		int TimePerData = 0;
		for (int i = 0; i < vtAlert.size(); i++)
		{
			Vector vt = (Vector) vtAlert.elementAt(i);

			if (Long.parseLong(vt.elementAt(0).toString()) == productid)
			{
				TimePerData = Integer.parseInt(vt.elementAt(4).toString());
				break;
			}
		}
		return TimePerData;
	}

	@SuppressWarnings("rawtypes")
	public String getBalanceName(long productid)
	{
		String balanceName = "";
		for (int i = 0; i < vtAlert.size(); i++)
		{
			Vector vt = (Vector) vtAlert.elementAt(i);

			if (Long.parseLong(vt.elementAt(0).toString()) == productid)
			{
				balanceName = vt.elementAt(2).toString();
				break;
			}
		}
		return balanceName;
	}

	@SuppressWarnings("rawtypes")
	public String getServiceAddress(long productid)
	{
		String serviceAddress = "";
		for (int i = 0; i < vtAlert.size(); i++)
		{
			Vector vt = (Vector) vtAlert.elementAt(i);

			if (Long.parseLong(vt.elementAt(0).toString()) == productid)
			{
				serviceAddress = vt.elementAt(3).toString();
				break;
			}
		}
		return serviceAddress;
	}
	
	public void setSQLCommand(String _sqlCommand)
	{
		this._sqlCommand = _sqlCommand;
	}

	public String getSQLCommand()
	{
		return _sqlCommand;
	}
	
	public int getRestTime() {
		return _restTime;
	}

	public void setRestTime(int _restTime) {
		this._restTime = _restTime;
	}
	
	public void setEnquireInterval(long enquireInterval)
	{
		this.enquireInterval = enquireInterval;
	}

	public long getEnquireInterval()
	{
		return enquireInterval;
	}
}
