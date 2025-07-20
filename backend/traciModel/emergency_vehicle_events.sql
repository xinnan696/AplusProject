use urbanflow;

DROP TABLE IF EXISTS emergency_vehicle_events;
CREATE TABLE emergency_vehicle_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id VARCHAR(64) NOT NULL UNIQUE COMMENT 'Unique ID of the scheduled event',
  event_type VARCHAR(32) NOT NULL COMMENT 'Type of event',
  vehicle_id VARCHAR(255) NOT NULL,
  vehicle_type VARCHAR(100) NOT NULL,
  organization VARCHAR(255) NOT NULL,
  trigger_time BIGINT NOT NULL,
  start_edge_id VARCHAR(255) NOT NULL,
  end_edge_id VARCHAR(255) NOT NULL,
  route_edges JSON NOT NULL,
  junctions_on_path JSON NOT NULL,
  signalized_junctions JSON NOT NULL,
  event_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'event status (pending, triggered, failed, ignored, completed)',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='Scheduled emergency vehicle events';


INSERT INTO emergency_vehicle_events (
    event_id,
    event_type,
    vehicle_id,
    vehicle_type,
    organization,
    trigger_time,
    start_edge_id,
    end_edge_id,
    route_edges,
    junctions_on_path,
    signalized_junctions,
    event_status
) VALUES (
    'emergency_event_1',          -- 'event_id': 事件的唯一ID
    'emergency_event',             -- 'event_type': 事件类型为“紧急响应”
    'e1',                        -- 'vehicle_id': 救护车ID
    'emergency',                      -- 'vehicle_type': 车辆类型
    'Dublin Emergency Services', -- 'organization': 所属组织
    6,                       -- 'trigger_time': 事件触发的时间戳
    '327046540',            -- 'start_edge_id': 起始路段ID
    '33924202',                 -- 'end_edge_id': 终点路段ID
    '["327046540", "1314474849", "143299839", "592071140#1", "25094842",
    "327046546", "1314507320", "1314507321#1", "25094845#1", "25094845#3",
    "25094845#4", "25094845#5", "25094845#6", "25094845#7", "25094845#8",
    "25094845#9", "1314507326", "1314507325#1", "38023762#1", "38023762#2",
    "38023762#3", "38023762#4", "38023762#5", "1314507334#1", "405573977#0",
    "405573977#1", "405573977#2", "405573977#3", "750298029#1", "750298029#3",
    "750298029#4", "750298029#5", "750298029#7", "33924202"]',  -- 'route_edges': 路径包含的路段ID 
    '["cluster_1420096799_47154676","cluster_5282839762_5528420376","2499777372","47154709",
      "47212016","47212033","1301899977","8732259243","cluster_8732259234_8732259241","47212053","8731771622",
      "47212058","8731771617","8731742414","8731771635","cluster_2455590523_389713_47212064",
      "8731668308","8734376330","8734376319","8734376326","8490209104","2455632801",
      "47212078","cluster_181310323_5380872437","47212095","47212099","8490216112",
      "8735988241","cluster_181310547_8735988243","8735988251","8735988254",
      "cluster_181310548_8756538123_8756538125","47212114"]', -- 'junctions_on_path': 路径包含的所有路口ID
    '["cluster_1420096799_47154676", "cluster_5282839762_5528420376", 
     "cluster_2455590523_389713_47212064", "cluster_181310323_5380872437"]', -- 'signalized_junctions': 路径包含的受信号灯控制的路口ID
    'pending'                         -- 'event_status': 事件状态，默认为“待处理”
);





