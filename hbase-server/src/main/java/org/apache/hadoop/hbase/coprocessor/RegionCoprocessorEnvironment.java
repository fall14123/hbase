/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hbase.coprocessor;

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.ExtendedCellBuilder;
import org.apache.hadoop.hbase.HBaseInterfaceAudience;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.RegionInfo;
import org.apache.hadoop.hbase.metrics.MetricRegistry;
import org.apache.hadoop.hbase.regionserver.OnlineRegions;
import org.apache.hadoop.hbase.regionserver.Region;
import org.apache.yetus.audience.InterfaceAudience;
import org.apache.yetus.audience.InterfaceStability;

@InterfaceAudience.LimitedPrivate(HBaseInterfaceAudience.COPROC)
@InterfaceStability.Evolving
public interface RegionCoprocessorEnvironment extends CoprocessorEnvironment<RegionCoprocessor> {
  /** @return the region associated with this coprocessor */
  Region getRegion();

  /** @return region information for the region this coprocessor is running on */
  RegionInfo getRegionInfo();

  /**
   * @return Interface to Map of regions online on this RegionServer {@link #getServerName()}}.
   */
  OnlineRegions getOnlineRegions();

  /** @return shared data between all instances of this coprocessor */
  ConcurrentMap<String, Object> getSharedData();

  /**
   * @return Hosting Server's ServerName
   */
  ServerName getServerName();

  /**
   * Be careful RPC'ing from a Coprocessor context.
   * RPC's will fail, stall, retry, and/or crawl because the remote side is not online, is
   * struggling or it is on the other side of a network partition. Any use of Connection from
   * inside a Coprocessor must be able to handle all such hiccups.
   *
   * <p>Using a Connection to get at a local resource -- say a Region that is on the local
   * Server or using Admin Interface from a Coprocessor hosted on the Master -- will result in a
   * short-circuit of the RPC framework to make a direct invocation avoiding RPC.
   *<p>
   * Note: If you want to create Connection with your own Configuration and NOT use the RegionServer
   * Connection (though its cache of locations will be warm, and its life-cycle is not the concern
   * of the CP), see {@link #createConnection(Configuration)}.
   * @return The host's Connection to the Cluster.
   */
  Connection getConnection();

  /**
   * Creates a cluster connection using the passed configuration.
   * <p>Using this Connection to get at a local resource -- say a Region that is on the local
   * Server or using Admin Interface from a Coprocessor hosted on the Master -- will result in a
   * short-circuit of the RPC framework to make a direct invocation avoiding RPC.
   * <p>
   * Note: HBase will NOT cache/maintain this Connection. If Coprocessors need to cache and reuse
   * this connection, it has to be done by Coprocessors. Also make sure to close it after use.
   *
   * @param conf configuration
   * @return Connection created using the passed conf.
   */
  Connection createConnection(Configuration conf) throws IOException;

  /**
   * Returns a MetricRegistry that can be used to track metrics at the region server level. All
   * metrics tracked at this level will be shared by all the coprocessor instances
   * of the same class in the same region server process. Note that there will be one
   * region coprocessor environment per region in the server, but all of these instances will share
   * the same MetricRegistry. The metric instances (like Counter, Timer, etc) will also be shared
   * among all of the region coprocessor instances.
   *
   * <p>See ExampleRegionObserverWithMetrics class in the hbase-examples modules to see examples of how
   * metrics can be instantiated and used.</p>
   * @return A MetricRegistry for the coprocessor class to track and export metrics.
   */
  // Note: we are not exposing getMetricRegistryForRegion(). per-region metrics are already costly
  // so we do not want to allow coprocessors to export metrics at the region level. We can allow
  // getMetricRegistryForTable() to allow coprocessors to track metrics per-table, per-regionserver.
  MetricRegistry getMetricRegistryForRegionServer();

  /**
   * Returns a CellBuilder so that coprocessors can build cells. These cells can also include tags.
   * Note that this builder does not support updating seqId of the cells
   * @return the ExtendedCellBuilder
   */
  ExtendedCellBuilder getCellBuilder();
}
