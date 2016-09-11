package com.yirendai.infra.ymock.dubbo.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MysqlManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(MysqlManager.class);

  // 开发环境数据库
  private DataSource originDataSource;
  // 自动化测试数据库
  private DataSource testDataSource;
  // 需要同步table schema的表
  private List<String> tableSchemaList = new ArrayList<String>();
  // 需要导数据的表
  private List<String> tableDataList = new ArrayList<String>();

  final ExecutorService exec = Executors.newFixedThreadPool(40);

  public MysqlManager(DataSource sourceSource, DataSource testSource) {
    this.originDataSource = sourceSource;
    this.testDataSource = testSource;
  }

  /**
   * 供集成测试使用！ 功能：同步所有的表结构，同步指定表的数据.
   * 
   */
  public void syncSuit() throws InterruptedException {
    List<String> allTestTables = getAllTables(testDataSource);

    CountDownLatch dropLatch = new CountDownLatch(1);
    // 第一步：删除测试环境的所有表
    asynDropTestTables(allTestTables, dropLatch);

    List<String> allOriginTables = getAllTables(originDataSource);
    List<String> createTableSQLList = exportCreateTableSQL(originDataSource, allOriginTables);
    List<String> insertSQLList = exportInsertSQL(tableDataList);
    dropLatch.await();

    // 第二步：导入开发环境的表结构
    importDataSchema(testDataSource, createTableSQLList);
    // 第三步：导入开发环境的表数据
    concurrentExecuteUpdate(insertSQLList);
  }

  /**
   * 供单元测试使用！ 功能：同步指定表的结构和数据.
   * 
   */
  public void syncCase() throws InterruptedException {
    CountDownLatch dropLatch = new CountDownLatch(1);

    Collections.sort(this.tableSchemaList);

    for (String tableName : tableDataList) {
      if (Collections.binarySearch(tableSchemaList, tableName) < 0) {
        tableSchemaList.add(tableName);
      }
    }

    // 第一步：删除测试环境的所有表
    asynDropTestTables(tableSchemaList, dropLatch);

    List<String> createTableSQLList = exportCreateTableSQL(originDataSource, tableSchemaList);
    List<String> insertSQLList = exportInsertSQL(tableDataList);
    dropLatch.await();

    // 第二步：导入开发环境的表结构
    importDataSchema(testDataSource, createTableSQLList);
    // 第三步：导入开发环境的表数据
    concurrentExecuteUpdate(insertSQLList);
  }

  public void setTableDataList(List<String> dataList) {
    this.tableDataList.clear();
    this.tableDataList.addAll(dataList);
  }

  private boolean contains(List<String> strList, String targetStr) {
    if (strList == null || strList.isEmpty() || targetStr == null) {
      return false;
    }

    for (String str : strList) {
      if (targetStr.equals(str)) {
        return true;
      }
    }

    return false;
  }

  /**
   * 同步表数据.
   */
  public void syncTableData(String tableName) {
    if (!contains(tableDataList, tableName)) {
      tableDataList.add(tableName);
    }
  }

  public void setTableSchemaList(List<String> tableSchemaList) {
    this.tableSchemaList.clear();
    this.tableSchemaList.addAll(tableSchemaList);
  }

  /**
   * 同步表结构.
   */
  public void syncTableSchema(String tableName) {
    if (!contains(tableSchemaList, tableName)) {
      tableSchemaList.add(tableName);
    }
  }

  /**
   * 获取所有的表名.
   */
  private List<String> getAllTables(DataSource dataSource) {
    List<String> result = new ArrayList<String>();
    ResultSet rs = null;
    try {
      Connection connection = dataSource.getConnection();
      DatabaseMetaData dbm = connection.getMetaData();
      String[] types = {"TABLE"};
      rs = dbm.getTables(null, null, "", types);
      while (rs.next()) {
        String str = rs.getString("TABLE_NAME");
        result.add(str);
      }
    } catch (SQLException ex) {
      LOGGER.error("getAllTables error", ex);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException ex) {
          LOGGER.error("closeConnection error", ex);
        }
      }
    }

    return result;
  }

  /**
   * 刪除测试环境中的表.
   *
   */
  private void asynDropTestTables(final List<String> tableList, final CountDownLatch latch) {
    Runnable run = new Runnable() {

      public void run() {
        try {
          dropTestTables(tableList);
        } finally {
          latch.countDown();
        }
      }
    };

    exec.submit(run);
  }

  private void dropTestTables(List<String> tableList) {
    if (tableList == null || tableList.isEmpty()) {
      return;
    }

    Connection connection = null;
    try {
      connection = testDataSource.getConnection();
      Statement stmt = connection.createStatement();
      for (String tableName : tableList) {
        String sourceSql = "DROP TABLE IF EXISTS " + tableName;
        stmt.addBatch(sourceSql);
      }
      stmt.executeBatch();

    } catch (SQLException ex) {
      LOGGER.error("dropTestTables error", ex);
    } finally {
      if (connection != null) {
        try {
          connection.close();
        } catch (SQLException ex) {
          LOGGER.error("closeConnection error", ex);
        }
      }
    }
  }

  private List<String> exportCreateTableSQL(DataSource dataSource, List<String> tableList) {
    List<String> retList = new ArrayList<String>();

    ResultSet rs = null;
    try {
      Connection connection = dataSource.getConnection();
      Statement stmt = connection.createStatement();

      for (String tableName : tableList) {
        String sourceSql = "show create table " + tableName;
        rs = stmt.executeQuery(sourceSql);

        while (rs.next()) {
          String createTableSql = rs.getString("Create Table");
          retList.add(createTableSql + ";");
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("exportCreateTableSQL error", ex);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException ex) {
          LOGGER.error("closeConnection error", ex);
        }
      }
    }

    return retList;
  }

  private void importDataSchema(DataSource dataSource, List<String> createSQLList) {
    if (createSQLList == null || createSQLList.isEmpty()) {
      return;
    }

    int batchSize = 20;

    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      Statement stmt = connection.createStatement();

      int num = 0;
      for (String createSQL : createSQLList) {
        num++;
        stmt.addBatch(createSQL);
        if (num >= batchSize) {
          stmt.executeBatch();
          num = 0;
        }
      }

      if (num > 0) {
        stmt.executeBatch();
      }
    } catch (SQLException ex) {
      LOGGER.error("importDataSchema error", ex);
    } finally {
      if (connection != null) {
        try {
          connection.close();
        } catch (SQLException ex) {
          LOGGER.error("closeConnection error", ex);
        }
      }
    }

  }

  private List<String> exportInsertSQL(List<String> tableList) {
    List<String> retList = new ArrayList<String>();
    if (tableList == null || tableList.isEmpty()) {
      return retList;
    }

    for (String table : tableList) {

      StringBuffer selectSQL = new StringBuffer().append("SELECT * FROM ").append(table);

      ResultSet rs = null;
      int rows = 0;
      Connection originConn = null;

      String insertSQL = null;
      try {
        originConn = originDataSource.getConnection();
        Statement originStatement = originConn.createStatement();
        rs = originStatement.executeQuery(selectSQL.toString());
        StringBuilder sb = new StringBuilder().append(this.generateSqlPrefix(table, rs));

        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();
        StringBuilder values = new StringBuilder();

        while (rs.next()) {
          rows++;
          values.append("(");
          for (int i = 1; i <= columnCount; i++) {
            String columValue = rs.getString(i);
            if (columValue == null) {
              values.append("null");
            } else {
              values.append("'").append(rs.getString(i)).append("'");
            }
            if (i != columnCount) {
              values.append(",");
            }
          }
          values.append("),");
        }

        if (rows > 0) {
          sb.append(values.substring(0, values.length() - 1)).append(";");
          insertSQL = sb.toString();
          retList.add(insertSQL);
        }

      } catch (SQLException ex) {
        LOGGER.error("exportInsertSQL error", ex);
      } finally {
        if (originConn != null) {
          try {
            originConn.close();
          } catch (SQLException ex) {
            LOGGER.error("closeConnection error", ex);
          }
        }
      }
    }

    return retList;
  }

  private String generateSqlPrefix(String table, ResultSet rs) {
    try {
      ResultSetMetaData rsmd = rs.getMetaData();
  

      StringBuffer insertSQL = new StringBuffer();
      insertSQL.append("INSERT INTO ");
      insertSQL.append(table);
      insertSQL.append("(");

      boolean first = true;
      int columnCount = rsmd.getColumnCount();
      for (int i = 1; i <= columnCount; i++) {
        {
          String column = rsmd.getColumnName(i);

          if (!first) {
            insertSQL.append(",");
          } else {
            first = false;
          }

          insertSQL.append(column);
        }
      }

      insertSQL.append(") VALUES ");

      return insertSQL.toString();
    } catch (SQLException ex) {
      LOGGER.error("generateSqlPrefix(" + table + ") error", ex);
    }

    return null;
  }

  /**
   * 执行update、delete、insert类型的SQL语句.
   */
  public void executeUpdate(String updateSQL) {
    if (updateSQL == null) {
      return;
    }

    Connection testConn = null;
    try {
      testConn = testDataSource.getConnection();
      Statement statement = testConn.createStatement();

      statement.executeUpdate(updateSQL);
    } catch (SQLException ex) {
      LOGGER.error("importData(" + updateSQL + ") error", ex);
    } finally {
      if (testConn != null) {
        try {
          testConn.close();
        } catch (SQLException ex) {
          LOGGER.error("closeConnection error", ex);
        }
      }
    }
  }

  /**
   * 并发执行SQL语句.
   * 
   */
  public void concurrentExecuteUpdate(List<String> sqlList) {
    if (sqlList == null || sqlList.isEmpty()) {
      return;
    }

    final CountDownLatch end = new CountDownLatch(sqlList.size());

    for (final String sql : sqlList) {
      Runnable run = new Runnable() {

        public void run() {
          try {
            executeUpdate(sql);
          } finally {
            end.countDown();
          }
        }
      };
      exec.submit(run);
    }

    try {
      end.await();
    } catch (InterruptedException ex) {
      LOGGER.error("concurrentExecuteUpdate error", ex);
    }
  }
}
