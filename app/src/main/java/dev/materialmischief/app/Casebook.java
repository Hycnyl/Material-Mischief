package dev.materialmischief.app;

final class Casebook {
    static final Casebook[] ALL = {
        new Casebook(
                "开关串供",
                "SWITCH",
                "点亮三个开关。它们似乎提前串好了口供。",
                "右邻监听器",
                "注意下一枚开关。你的手指只碰了一枚，亮灯数却可能变化两次。",
                "每枚开关会翻转自己和右邻。从全灭开始，依次点第一枚和第三枚。",
                "开关承认，右手边的同事一直在偷听。"),
        new Casebook(
                "滑块罢工",
                "SLIDER",
                "把读数调到 70%。拇指今天拒绝挪动。",
                "带薪休息的拇指",
                "拇指钉住了。试试看，它脚下的刻度能不能动？",
                "向左拖动刻度，让 70 对准中间的指针，再松手。也可以用步进按钮微调。",
                "刻度尺完成了整根进度条的工作。拇指照常领薪。"),
        new Casebook(
                "撤销套娃",
                "SNACKBAR",
                "从系统回执里找回那枚被归档的彩蛋。",
                "退回的回执",
                "有一条回执，专门记着另一条回执的去向。",
                "翻到第三条，撤销「收起回执」，再撤销里面的彩蛋归档操作。",
                "撤销了撤销的撤销。彩蛋安全到案。"),
        new Casebook(
                "浮钮越狱",
                "FAB",
                "把逃跑的浮动按钮接回底座。",
                "在逃加号",
                "先看它往哪里跑。可以移动的是底座。",
                "初始时，点右上方空站放好底座，再点左上的浮钮。它会自己跑进去。",
                "浮钮坚持声称，这只是一次正常的位置动画。"),
        new Casebook(
                "通知叠叠乐",
                "NOTIFICATION",
                "依据通知里的线索，复原早晨发生的顺序。",
                "早班通知栈",
                "读读每张卡片。哪件事把人叫醒？又是哪件事等所有人安静才发生？",
                "按闹钟、截图、勿扰模式、系统更新的顺序从上往下排列，再归档。",
                "四条相互推卸责任的通知，终于对上了时间线。"),
        new Casebook(
                "版本号之外",
                "ABOUT ANDROID",
                "在「关于手机」里，敲开本次更新真正的彩蛋。",
                "迟到的彩蛋",
                "老 Android 用户的手指记得，版本号可以多点几下。留心打卡时间。",
                "连续点三次 Android 版本，把钟拨到 09:15，再敲开蛋壳。",
                "更新日志终于多了一行：新增一枚愿意被发现的彩蛋。")
    };

    final String name, component, brief, collectible, hint, solution, report;

    Casebook(
            String name,
            String component,
            String brief,
            String collectible,
            String hint,
            String solution,
            String report) {
        this.name = name;
        this.component = component;
        this.brief = brief;
        this.collectible = collectible;
        this.hint = hint;
        this.solution = solution;
        this.report = report;
    }

    static String number(int i) {
        return "0" + (i + 1);
    }
}
