export const workbenchMainFlowManualSpec = {
  start: "打开 http://127.0.0.1:5173",
  firstInput: "今天喝了一支水洗埃塞，有柑橘和红茶感",
  followupInput: "豆子是某烘焙商的埃塞水洗豆，水温 92 度，粉水比 1:15，想看克制、夸张和锐评。",
  expected: ["助手追问缺失事实", "三版草稿可见", "事实边界说明可见"]
};
