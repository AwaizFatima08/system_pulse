import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def create_app_icon():
    # Render at 4x resolution (2048x2048) and scale down to 512x512 with LANCZOS
    size = 2048
    img = Image.new("RGBA", (size, size), (10, 14, 23, 255)) # #0A0E17
    draw = ImageDraw.Draw(img)

    center = (size // 2, size // 2)
    radius = size * 0.40

    # 1. Subtle radial glow behind gauge
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    for r in range(int(radius * 1.25), int(radius * 0.7), -15):
        alpha = int(45 * (1.0 - (r - radius * 0.7) / (radius * 0.55)))
        glow_draw.ellipse(
            [center[0] - r, center[1] - r, center[0] + r, center[1] + r],
            fill=(0, 240, 255, max(0, min(alpha, 50)))
        )
    glow = glow.filter(ImageFilter.GaussianBlur(radius=30))
    img.alpha_composite(glow)
    draw = ImageDraw.Draw(img)

    # 2. Outer Speed Gauge Track (240 degrees from 150 to 390)
    track_width = int(size * 0.055)
    bbox = [center[0] - radius, center[1] - radius, center[0] + radius, center[1] + radius]
    draw.arc(bbox, start=150, end=390, fill=(28, 39, 60, 255), width=track_width)

    # 3. Dynamic Multi-colored Glowing Active Arc
    # Gradient from Cyan (0, 240, 255) -> Violet (157, 78, 221) -> Green (0, 230, 118)
    steps = 140
    start_deg = 150
    sweep_deg = 200 # active speed up to ~85%
    for i in range(steps):
        deg1 = start_deg + (sweep_deg * i / steps)
        deg2 = start_deg + (sweep_deg * (i + 1) / steps) + 0.5
        t = i / steps
        if t < 0.5:
            # Cyan to Violet
            ratio = t / 0.5
            r = int(0 * (1 - ratio) + 157 * ratio)
            g = int(240 * (1 - ratio) + 78 * ratio)
            b = int(255 * (1 - ratio) + 221 * ratio)
        else:
            # Violet to Green
            ratio = (t - 0.5) / 0.5
            r = int(157 * (1 - ratio) + 0 * ratio)
            g = int(78 * (1 - ratio) + 230 * ratio)
            b = int(221 * (1 - ratio) + 118 * ratio)
        draw.arc(bbox, start=deg1, end=deg2, fill=(r, g, b, 255), width=track_width)

    # 4. Tick marks
    tick_count = 11
    tick_outer = radius - track_width * 0.75
    tick_inner = radius - track_width * 1.35
    for i in range(tick_count):
        deg = 150 + (240 * i / (tick_count - 1))
        rad = math.radians(deg)
        x1 = center[0] + tick_outer * math.cos(rad)
        y1 = center[1] + tick_outer * math.sin(rad)
        x2 = center[0] + tick_inner * math.cos(rad)
        y2 = center[1] + tick_inner * math.sin(rad)
        color = (0, 240, 255, 255) if i <= 8 else (88, 96, 105, 180)
        draw.line([(x1, y1), (x2, y2)], fill=color, width=int(size * 0.009))

    # 5. Heartbeat / Network Pulse Waveform in center
    wave_pts = [
        (center[0] - size * 0.28, center[1] + size * 0.05),
        (center[0] - size * 0.15, center[1] + size * 0.05),
        (center[0] - size * 0.10, center[1] - size * 0.02),
        (center[0] - size * 0.05, center[1] + size * 0.14),
        (center[0] + size * 0.02, center[1] - size * 0.15),
        (center[0] + size * 0.08, center[1] + size * 0.08),
        (center[0] + size * 0.13, center[1] + size * 0.05),
        (center[0] + size * 0.28, center[1] + size * 0.05),
    ]
    # Glow for waveform
    wave_glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    wave_draw = ImageDraw.Draw(wave_glow)
    wave_draw.line(wave_pts, fill=(0, 240, 255, 180), width=int(size * 0.026), joint="curve")
    wave_glow = wave_glow.filter(ImageFilter.GaussianBlur(radius=16))
    img.alpha_composite(wave_glow)
    draw = ImageDraw.Draw(img)
    draw.line(wave_pts, fill=(240, 246, 252, 255), width=int(size * 0.015), joint="curve")

    # 6. Central Speedometer Hub
    hub_radius = int(size * 0.055)
    draw.ellipse(
        [center[0] - hub_radius, center[1] - hub_radius, center[0] + hub_radius, center[1] + hub_radius],
        fill=(19, 27, 42, 255),
        outline=(0, 240, 255, 255),
        width=int(size * 0.01)
    )
    inner_hub = int(size * 0.024)
    draw.ellipse(
        [center[0] - inner_hub, center[1] - inner_hub, center[0] + inner_hub, center[1] + inner_hub],
        fill=(0, 240, 255, 255)
    )

    # 7. Speedometer Needle pointing at 320 degrees (high speed)
    needle_angle = math.radians(315)
    needle_len = radius * 0.78
    tip_x = center[0] + needle_len * math.cos(needle_angle)
    tip_y = center[1] + needle_len * math.sin(needle_angle)
    base_perp = needle_angle + math.pi / 2
    base_w = size * 0.025
    b1_x = center[0] + base_w * math.cos(base_perp)
    b1_y = center[1] + base_w * math.sin(base_perp)
    b2_x = center[0] - base_w * math.cos(base_perp)
    b2_y = center[1] - base_w * math.sin(base_perp)
    draw.polygon([(tip_x, tip_y), (b1_x, b1_y), (b2_x, b2_y)], fill=(0, 230, 118, 255))

    # Resize to final 512x512
    icon_512 = img.resize((512, 512), Image.Resampling.LANCZOS)
    return icon_512

def create_feature_graphic():
    # 1024 x 500
    width = 1024
    height = 500
    img = Image.new("RGBA", (width, height), (10, 14, 23, 255))
    draw = ImageDraw.Draw(img)

    # 1. Subtle cyber grid background
    grid_color = (20, 28, 44, 255)
    for x in range(0, width, 40):
        draw.line([(x, 0), (x, height)], fill=grid_color, width=1)
    for y in range(0, height, 40):
        draw.line([(0, y), (width, y)], fill=grid_color, width=1)

    # 2. Glowing atmospheric orbs (Cyan on left, Violet on right)
    orb_layer = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    orb_draw = ImageDraw.Draw(orb_layer)
    orb_draw.ellipse([-100, -100, 450, 450], fill=(0, 240, 255, 35))
    orb_draw.ellipse([width - 450, 50, width + 150, height + 250], fill=(157, 78, 221, 40))
    orb_layer = orb_layer.filter(ImageFilter.GaussianBlur(radius=60))
    img.alpha_composite(orb_layer)
    draw = ImageDraw.Draw(img)

    # 3. Floating Speedometer on the right side
    gauge_center = (width - 240, height // 2)
    gauge_r = 160
    bbox = [gauge_center[0] - gauge_r, gauge_center[1] - gauge_r, gauge_center[0] + gauge_r, gauge_center[1] + gauge_r]
    draw.arc(bbox, start=150, end=390, fill=(30, 42, 65, 255), width=24)
    # Active arc
    draw.arc(bbox, start=150, end=340, fill=(0, 240, 255, 255), width=24)

    # Needle
    needle_angle = math.radians(325)
    tip_x = gauge_center[0] + (gauge_r * 0.75) * math.cos(needle_angle)
    tip_y = gauge_center[1] + (gauge_r * 0.75) * math.sin(needle_angle)
    draw.line([gauge_center, (tip_x, tip_y)], fill=(0, 230, 118, 255), width=6)
    draw.ellipse([gauge_center[0] - 18, gauge_center[1] - 18, gauge_center[0] + 18, gauge_center[1] + 18], fill=(0, 240, 255, 255))

    # Center readout
    font_large = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 46)
    font_sub = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 15)
    draw.text((gauge_center[0] - 78, gauge_center[1] + 28), "450.2", font=font_large, fill=(240, 246, 252, 255))
    draw.text((gauge_center[0] - 25, gauge_center[1] + 80), "Mbps", font=font_sub, fill=(0, 240, 255, 255))

    # 4. Left side Branding & Value Proposition
    font_title = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 52)
    font_tagline = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 18)
    font_feature = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 14)

    # Title with subtle glow
    draw.text((65, 110), "SYSTEM PULSE", font=font_title, fill=(0, 240, 255, 255))
    draw.text((65, 180), "NEXT-GEN NETWORK & ISP DIAGNOSTICIAN", font=font_tagline, fill=(157, 78, 221, 255))

    # Feature badges
    badges = [
        "⚡ Dual-Tier Disambiguation: Wi-Fi vs. ISP",
        "🎮 Bufferbloat & Loaded Latency Grading (A+ to F)",
        "📺 Instant 4K Streaming & Gaming QoE Scoring",
        "🗺️ Crowdsourced ISP Speed & Heatmap Explorer"
    ]
    y_pos = 245
    for badge in badges:
        # Background pill
        draw.rounded_rectangle([65, y_pos, 520, y_pos + 36], radius=8, fill=(19, 27, 42, 220), outline=(35, 48, 72, 255), width=1)
        draw.text((80, y_pos + 9), badge, font=font_feature, fill=(240, 246, 252, 255))
        y_pos += 46

    # Convert to RGB (Play Store requirement: no alpha in feature graphic)
    rgb_img = Image.new("RGB", (width, height), (10, 14, 23))
    rgb_img.paste(img, (0, 0), img)
    return rgb_img

if __name__ == "__main__":
    out_dir = "/mnt/storage/projects/system_pulse/graphics"
    os.makedirs(out_dir, exist_ok=True)

    # 1. 512x512 Play Store Icon
    icon = create_app_icon()
    icon_path = os.path.join(out_dir, "play_store_icon_512.png")
    icon.save(icon_path, "PNG")
    print(f"Saved {icon_path} (512x512)")

    # 2. 1024x500 Play Store Feature Graphic
    feature = create_feature_graphic()
    feature_path = os.path.join(out_dir, "feature_graphic_1024x500.png")
    feature.save(feature_path, "PNG")
    print(f"Saved {feature_path} (1024x500)")

    # 3. Android mipmap launcher icons
    res_dir = "/mnt/storage/projects/system_pulse/app/src/main/res"
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for folder, dim in densities.items():
        folder_path = os.path.join(res_dir, folder)
        os.makedirs(folder_path, exist_ok=True)
        scaled = icon.resize((dim, dim), Image.Resampling.LANCZOS)
        scaled.save(os.path.join(folder_path, "ic_launcher.png"), "PNG")
        scaled.save(os.path.join(folder_path, "ic_launcher_round.png"), "PNG")
        print(f"Updated {folder}/ic_launcher.png ({dim}x{dim})")
