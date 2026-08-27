# 极坐标裂隙武器接口

`data.scripts.weapons.PTSDPolarRiftRenderer` 是裂隙形状的共用入口。

- `Shape(seed, segments, spikeCount, spikeStrength)` 创建可复现的随机边缘。
- `render(...)` 依照 `r(theta,t)=base*star(theta)*noise(theta,t)` 绘制黑色内核与紫色边缘。
- 四向坍缩使用 `spikeCount=4`；`|cos(2θ)|` 在 0/90/180/270 度取得峰值。
- `stretch` 从 0 到 1，使形状沿 `facing` 方向由带刺圆形拉成长椭圆。

后续裂隙武器应优先复用此入口，通过不同尖刺数、强度、谐波和时间参数形成差异，避免连续堆叠大量贴图粒子。