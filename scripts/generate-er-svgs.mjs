import { writeFileSync } from "node:fs";

const style = `
  <style>
    svg { background: #050505; font-family: "Microsoft YaHei", "PingFang SC", Arial, sans-serif; }
    .entity rect { fill: #f7f7f7; stroke: #222; stroke-width: 2; }
    .attr ellipse { fill: #fff; stroke: #222; stroke-width: 1.4; }
    .rel polygon { fill: #fff; stroke: #222; stroke-width: 1.4; }
    .line { stroke: #7a3030; stroke-width: 1.2; fill: none; }
    text { fill: #111; text-anchor: middle; dominant-baseline: middle; font-size: 15px; }
    .entity text, .rel text { font-weight: 700; }
    .card { fill: #fff; font-size: 18px; font-weight: 700; paint-order: stroke; stroke: #050505; stroke-width: 4px; }
  </style>`;

const entities = {
  app_user: {
    name: "普通用户",
    attrs: ["用户编号", "账号名称", "密码", "昵称", "邮箱", "手机号", "账号状态", "删除标记", "创建时间", "更新时间"],
  },
  admin: {
    name: "管理员",
    attrs: ["管理员编号", "账号名称", "密码", "角色", "删除标记", "创建时间", "更新时间"],
  },
  car_model: {
    name: "车型基础信息",
    attrs: ["车型编号", "品牌", "车系", "车型名称", "指导价", "车身类型", "动力类型", "座位数", "上市年份", "图片地址", "销量", "用户评分", "审核状态", "删除标记", "创建时间", "更新时间"],
  },
  car_image_asset: {
    name: "车型图片资源",
    attrs: ["图片资源编号", "车型编号", "原始文件名", "存储文件名", "文件类型", "文件大小", "图片宽度", "图片高度", "访问地址", "存储路径", "校验值", "审核状态", "拒绝原因", "上传管理员编号", "审核管理员编号", "删除标记", "创建时间", "更新时间", "审核时间"],
  },
  car_param: {
    name: "车型参数",
    attrs: ["参数编号", "车型编号", "车长", "车宽", "车高", "轴距", "燃油消耗", "电耗", "纯电续航", "综合续航", "百公里加速", "安全气囊数量", "ABS配置", "ESP配置", "主动刹车配置", "车道保持配置", "自适应巡航配置", "盲区辅助配置", "倒车影像配置", "360全景影像配置", "OTA配置", "语音控制配置", "自动泊车配置", "中控屏尺寸", "辅助驾驶级别", "删除标记", "创建时间", "更新时间"],
  },
  car_feature_score: {
    name: "车型特征评分",
    attrs: ["评分编号", "车型编号", "空间分", "安全分", "能耗分", "智能分", "舒适分", "动力分", "口碑分", "热度分", "评分版本", "计算时间", "删除标记", "创建时间", "更新时间"],
  },
  user_demand: {
    name: "用户购车需求",
    attrs: ["需求编号", "用户编号", "原始文本", "预算下限", "预算上限", "品牌偏好", "车身类型偏好", "动力类型偏好", "座位选项", "最低座位数", "使用场景", "因素权重", "排除品牌", "排除车型编号", "用户画像文本", "价格权重", "空间权重", "安全权重", "能耗权重", "智能权重", "舒适权重", "动力权重", "口碑权重", "热度权重", "删除标记", "创建时间", "更新时间"],
  },
  recommend_record: {
    name: "推荐记录",
    attrs: ["推荐记录编号", "用户编号", "需求编号", "用户画像快照", "权重快照", "补充推荐提示", "推荐状态", "删除标记", "创建时间", "更新时间"],
  },
  recommend_item: {
    name: "推荐明细",
    attrs: ["推荐明细编号", "推荐记录编号", "车型编号", "排名", "综合推荐分", "价格分", "空间分", "安全分", "能耗分", "智能分", "舒适分", "动力分", "口碑分", "热度分", "推荐标签", "匹配层级", "推荐理由", "不足提醒", "删除标记", "创建时间", "更新时间"],
  },
  user_favorite: {
    name: "用户收藏",
    attrs: ["收藏编号", "用户编号", "车型编号", "删除标记", "创建时间", "更新时间"],
  },
  user_compare_car: {
    name: "用户车型对比",
    attrs: ["对比编号", "用户编号", "车型编号", "排序号", "删除标记", "创建时间", "更新时间"],
  },
  recommend_feedback: {
    name: "推荐反馈",
    attrs: ["反馈编号", "用户编号", "推荐记录编号", "满意度分值", "满意度类型", "原因标签", "反馈内容", "删除标记", "创建时间", "更新时间"],
  },
};

const diagrams = [
  {
    file: "docs/er-account-user.svg",
    width: 3000,
    height: 2100,
    nodes: {
      app_user: [420, 430],
      admin: [1500, 360],
      user_favorite: [940, 1320],
      user_compare_car: [1900, 1320],
      recommend_feedback: [2580, 760],
      car_model_ref: [1500, 1780, "车型基础信息", []],
      recommend_record_ref: [2580, 1500, "推荐记录", []],
    },
    relations: [
      ["app_user", "user_favorite", "用户收藏车型", "1", "N"],
      ["app_user", "user_compare_car", "用户加入对比", "1", "N"],
      ["app_user", "recommend_feedback", "用户提交反馈", "1", "N"],
      ["car_model_ref", "user_favorite", "车型被收藏", "1", "N"],
      ["car_model_ref", "user_compare_car", "车型被对比", "1", "N"],
      ["recommend_record_ref", "recommend_feedback", "推荐获得反馈", "1", "N"],
    ],
  },
  {
    file: "docs/er-car-data.svg",
    width: 3600,
    height: 2400,
    nodes: {
      admin: [420, 380],
      car_model: [1740, 500],
      car_image_asset: [3020, 620],
      car_param: [900, 1760],
      car_feature_score: [2500, 1760],
    },
    relations: [
      ["admin", "car_image_asset", "管理员上传图片", "1", "N"],
      ["admin", "car_image_asset", "管理员审核图片", "1", "N", 120],
      ["car_model", "car_image_asset", "车型拥有图片", "1", "N"],
      ["car_model", "car_param", "车型具有参数", "1", "1"],
      ["car_model", "car_feature_score", "车型生成评分", "1", "1"],
    ],
  },
  {
    file: "docs/er-recommendation.svg",
    width: 3700,
    height: 2500,
    nodes: {
      app_user: [420, 450],
      user_demand: [1500, 460],
      recommend_record: [2700, 460],
      recommend_item: [1800, 1720],
      car_model: [600, 1720],
      recommend_feedback: [3100, 1720],
    },
    relations: [
      ["app_user", "user_demand", "用户提交需求", "1", "N"],
      ["app_user", "recommend_record", "用户生成推荐", "1", "N"],
      ["user_demand", "recommend_record", "需求生成推荐", "1", "N"],
      ["recommend_record", "recommend_item", "推荐包含明细", "1", "N"],
      ["car_model", "recommend_item", "车型进入推荐明细", "1", "N"],
      ["app_user", "recommend_feedback", "用户提交反馈", "1", "N"],
      ["recommend_record", "recommend_feedback", "推荐获得反馈", "1", "N"],
    ],
  },
];

function esc(value) {
  return String(value).replace(/[&<>"]/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[char]);
}

function text(label, x, y, width = 150, size = 15, cls = "") {
  const max = Math.max(4, Math.floor(width / size * 1.65));
  if (label.length <= max) {
    return `<text x="${x}" y="${y}"${cls ? ` class="${cls}"` : ""}>${esc(label)}</text>`;
  }
  const first = label.slice(0, max);
  const second = label.slice(max);
  return `<text x="${x}" y="${y}"${cls ? ` class="${cls}"` : ""}><tspan x="${x}" dy="-7">${esc(first)}</tspan><tspan x="${x}" dy="17">${esc(second)}</tspan></text>`;
}

function entity(name, x, y) {
  return `<g class="entity"><rect x="${x - 85}" y="${y - 35}" width="170" height="70"/>${text(name, x, y, 150, 16)}</g>`;
}

function attr(label, x, y) {
  const rx = Math.max(56, Math.min(92, label.length * 10));
  const ry = label.length > 7 ? 34 : 28;
  return `<g class="attr"><ellipse cx="${x}" cy="${y}" rx="${rx}" ry="${ry}"/>${text(label, x, y, rx * 1.7)}</g>`;
}

function diamond(label, x, y) {
  const w = Math.max(170, label.length * 18);
  const h = 74;
  return `<g class="rel"><polygon points="${x},${y - h / 2} ${x + w / 2},${y} ${x},${y + h / 2} ${x - w / 2},${y}"/>${text(label, x, y, w - 20, 15)}</g>`;
}

function line(x1, y1, x2, y2) {
  return `<line class="line" x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}"/>`;
}

function card(label, x1, y1, x2, y2, ratio) {
  const x = x1 + (x2 - x1) * ratio;
  const y = y1 + (y2 - y1) * ratio - 13;
  return `<text class="card" x="${x}" y="${y}">${label}</text>`;
}

function attributePositions(cx, cy, count) {
  const top = Math.ceil(count / 4);
  const right = Math.ceil((count - top) / 3);
  const bottom = Math.ceil((count - top - right) / 2);
  const left = count - top - right - bottom;
  const positions = [];
  const addRow = (n, y, startX, gap) => {
    for (let i = 0; i < n; i += 1) positions.push([startX + i * gap, y]);
  };
  const addCol = (n, x, startY, gap) => {
    for (let i = 0; i < n; i += 1) positions.push([x, startY + i * gap]);
  };
  addRow(top, cy - 250, cx - ((top - 1) * 145) / 2, 145);
  addCol(right, cx + 315, cy - ((right - 1) * 92) / 2, 92);
  addRow(bottom, cy + 250, cx - ((bottom - 1) * 145) / 2, 145);
  addCol(left, cx - 315, cy - ((left - 1) * 92) / 2, 92);
  return positions;
}

for (const diagram of diagrams) {
  const parts = [
    `<svg xmlns="http://www.w3.org/2000/svg" width="${diagram.width}" height="${diagram.height}" viewBox="0 0 ${diagram.width} ${diagram.height}">`,
    style,
  ];
  const coords = {};

  for (const [id, value] of Object.entries(diagram.nodes)) {
    const [x, y, customName, customAttrs] = value;
    const data = customName ? { name: customName, attrs: customAttrs } : entities[id];
    coords[id] = { x, y };
    const positions = attributePositions(x, y, data.attrs.length);
    data.attrs.forEach((label, index) => {
      const [ax, ay] = positions[index];
      parts.push(line(x, y, ax, ay));
      parts.push(attr(label, ax, ay));
    });
  }

  diagram.relations.forEach(([fromId, toId, label, fromCard, toCard, offset = 0]) => {
    const from = coords[fromId];
    const to = coords[toId];
    const mx = (from.x + to.x) / 2;
    const my = (from.y + to.y) / 2 + offset;
    parts.push(line(from.x, from.y, mx, my));
    parts.push(line(mx, my, to.x, to.y));
    parts.push(card(fromCard, from.x, from.y, mx, my, 0.72));
    parts.push(card(toCard, mx, my, to.x, to.y, 0.28));
    parts.push(diamond(label, mx, my));
  });

  for (const [id, value] of Object.entries(diagram.nodes)) {
    const [x, y, customName] = value;
    parts.push(entity(customName || entities[id].name, x, y));
  }

  parts.push("</svg>");
  writeFileSync(diagram.file, parts.join("\n"), "utf8");
  console.log(`wrote ${diagram.file}`);
}
