/*
 * Copyright (c) 2013 Websquared, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     swsong - initial API and implementation
 */

package org.fastcatsearch.db.object;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class IndexingSchedule extends DAOBase {

	public String collection;
	public String type;
	public int period;
	public Timestamp startTime;
	public boolean isActive;
	
	public IndexingSchedule(){ }
	
//	public int createBody(Statement stmt) throws SQLException
//	{
//		String createSQL = "create table " + tableName + "(collection varchar(20), type char(1), period int, startTime timestamp, isActive smallint)";
//		return stmt.executeUpdate(createSQL);
//	}
	
	public int create() throws SQLException {
		Connection conn = null;
		Statement stmt = null;
		String createSQL = "";
		try
		{
			conn = conn();
			createSQL = "create table " + tableName + "(collection varchar(20), type char(1), period int, startTime timestamp, isActive smallint)";
			stmt = conn.createStatement();
			return stmt.executeUpdate(createSQL); 
		} finally {
			releaseResource(stmt);
			releaseConnection(conn);			
		}
	}
	
	public int delete(String collection)
	{
		Connection conn = null;
		int result = 0;
		PreparedStatement pstmt = null;
		try
		{
			conn = conn();
			String deleteSQL = "delete from "  + tableName + " where collection = ?";
			pstmt = conn.prepareStatement(deleteSQL);
			int parameterIndex = 1;
			pstmt.setString(parameterIndex++, collection);
			result = pstmt.executeUpdate();	}
		catch ( Exception e) {
			e.printStackTrace();
		} finally {
			releaseResource(pstmt);
			releaseConnection(conn);						
		}
		return result;
	}
	
	public int deleteByType(String collection, String type)
	{
		String deleteSQL = "delete from "  + tableName + " where collection = ? and type = ? ";
		
		int result = 0;
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = conn();
			pstmt = conn.prepareStatement(deleteSQL);
			int parameterIndex = 1;
			pstmt.setString(parameterIndex++, collection);
			pstmt.setString(parameterIndex++, type);
			result = pstmt.executeUpdate(); }
		catch ( Exception e) {
			e.printStackTrace();
		} finally {
			releaseResource(pstmt);
			releaseConnection(conn);
		}
		return result;
	}
	
	public int updateOrInsert(String collection, String type, int period, Timestamp startTime, boolean isActive) {
		String checkSQL = "select count(collection) from " + tableName + " " +
				"where collection=? and type=?";
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int result = 0;		
		
		try{
			conn = conn();
			pstmt = conn.prepareStatement(checkSQL);
			int parameterIndex = 1;
			pstmt.setString(parameterIndex++, collection);
			pstmt.setString(parameterIndex++, type);
			rs = pstmt.executeQuery();
			int count = 0;
			if(rs.next()){
				count = rs.getInt(1);
			}
			
			if(count > 0){
				result =  update(collection, type, period, startTime, isActive);
			}else{
				result =  insert(collection, type, period, startTime, isActive);
			}			
		} catch(SQLException e){
			logger.error(e.getMessage(),e);
			result =  -1;
		} finally {
			releaseResource(pstmt, rs);
			releaseConnection(conn);
		}
		return result;
	}
	
	public int updateStatus(String collection, String type, boolean isActive) {
		int result = -1;
		String updateSQL = "update " + tableName + " set isActive=? " +
				"where collection=? and type=?";
		
		PreparedStatement pstmt = null;
		Connection conn = null;
		
		try{
			conn = conn();
			pstmt = conn.prepareStatement(updateSQL);
			int parameterIndex = 1;
			pstmt.setBoolean(parameterIndex++, isActive);
			pstmt.setString(parameterIndex++, collection);
			pstmt.setString(parameterIndex++, type);
			result =  pstmt.executeUpdate();
		} catch(SQLException e){
			logger.error(e.getMessage(),e);
			result =  -1;
		} finally {
			releaseResource(pstmt);
			releaseConnection(conn);
		}
		return result;
	}

	public int insert(String collection, String type, int period, Timestamp startTime, boolean isActive) {
		int result = -1;
		String insertSQL = "insert into " + tableName + "(collection, type, period, startTime, isActive) values (?,?,?,?,?)";
		
		PreparedStatement pstmt = null;
		Connection conn = null;
		
		try{
			conn = conn();
			pstmt = conn.prepareStatement(insertSQL);
			int parameterIndex = 1;
			pstmt.setString(parameterIndex++, collection);
			pstmt.setString(parameterIndex++, type);
			pstmt.setInt(parameterIndex++, period);
			pstmt.setTimestamp(parameterIndex++, startTime);
			pstmt.setBoolean(parameterIndex++, isActive);
			result =  pstmt.executeUpdate();
		} catch(SQLException e){
			logger.error(e.getMessage(),e);
			result = -1;
		} finally {
			releaseResource(pstmt);
			releaseConnection(conn);
		}
		return result;
	}
	
	public int update(String collection, String type, int period, Timestamp startTime, boolean isActive) {
		int result = -1;
		String updateSQL = "update " + tableName + " set period=?, startTime=?, isActive=? " +
				"where collection=? and type=?";
		PreparedStatement pstmt = null;
		Connection conn = null;
		try{
			conn = conn();
			pstmt = conn.prepareStatement(updateSQL);
			int parameterIndex = 1;
			pstmt.setInt(parameterIndex++, period);
			pstmt.setTimestamp(parameterIndex++, startTime);
			pstmt.setBoolean(parameterIndex++, isActive);
			pstmt.setString(parameterIndex++, collection);
			pstmt.setString(parameterIndex++, type);
			result =  pstmt.executeUpdate();
		} catch(SQLException e){
			logger.error(e.getMessage(),e);
			result = -1;
		} finally {
			releaseResource(pstmt);
			releaseConnection(conn);
		}
		return result;
	}
	
	public IndexingSchedule select(String collection, String type) {
		String selectSQL = "select collection, type, period, startTime, isActive from " + tableName + " " +
				"where collection=? and type=?";
		
		IndexingSchedule r = null;
		PreparedStatement pstmt = null;
		Connection conn = null;
		ResultSet rs = null;

		try {
			conn = conn();
			pstmt = conn.prepareStatement(selectSQL);
			
			int parameterIndex = 1;
			pstmt.setString(parameterIndex++, collection);
			pstmt.setString(parameterIndex++, type);
			rs = pstmt.executeQuery();
			
			while(rs.next()){
				r = new IndexingSchedule();
				
				parameterIndex = 1;
				r.collection = rs.getString(parameterIndex++);
				r.type = rs.getString(parameterIndex++);
				r.period = rs.getInt(parameterIndex++);
				r.startTime = rs.getTimestamp(parameterIndex++);
				r.isActive = rs.getBoolean(parameterIndex++);
			}
		
		} catch(SQLException e){
			logger.error(e.getMessage(),e);
		} finally {
			releaseResource(pstmt, rs);
			releaseConnection(conn);
		}
		return r;
	}
	
	public List<IndexingSchedule> selectAll() {
		String selectSQL = "select collection, type, period, startTime, isActive from " + tableName + " where isActive = 1";
		
		List<IndexingSchedule> result = new ArrayList<IndexingSchedule>();
		PreparedStatement pstmt = null;
		Connection conn = null;
		ResultSet rs = null;
		
		try{
			conn = conn();
			pstmt = conn.prepareStatement(selectSQL);
			rs = pstmt.executeQuery();
			
			while(rs.next()){
				IndexingSchedule r = new IndexingSchedule();
				
				int parameterIndex = 1;
				r.collection = rs.getString(parameterIndex++);
				r.type = rs.getString(parameterIndex++);
				r.period = rs.getInt(parameterIndex++);
				r.startTime = rs.getTimestamp(parameterIndex++);
				r.isActive = rs.getBoolean(parameterIndex++);
				
				result.add(r);
			}
		} catch(SQLException e){
			logger.error(e.getMessage(),e);
		} finally {
			releaseResource(pstmt, rs);
			releaseConnection(conn);
		}
		return result;
	}
	
	public int testAndCreate() throws SQLException {
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		if ( isExists() == false )
			create();
		
		try {
			conn = conn();
			pstmt = conn.prepareStatement("select count(*) from " + tableName);
			rs = pstmt.executeQuery();
			rs.next();
			return 0;
		} catch (SQLException e) {
			create();
			return 1;
		} finally {
			releaseResource(pstmt, rs);
			releaseConnection(conn);
		}
	}
}
