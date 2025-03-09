package org.xhy.function_calling.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Web界面控制器
 * 提供前端HTML页面
 */
@Controller
public class WebController {

    /**
     * 首页
     * 
     * @return ModelAndView对象，指向index模板
     */
    @GetMapping("/")
    public ModelAndView index() {
        return new ModelAndView("index");
    }

    /**
     * 重定向/index到根路径
     * 
     * @return 重定向指令
     */
    @GetMapping("/index")
    public String redirectToRoot() {
        return "redirect:/";
    }
}