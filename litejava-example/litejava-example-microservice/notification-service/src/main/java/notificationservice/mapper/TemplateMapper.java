package notificationservice.mapper;

import notificationservice.model.Template;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface TemplateMapper {
    
    @Select("SELECT * FROM templates WHERE code = #{code}")
    Template findByCode(String code);
    
    @Select("SELECT * FROM templates")
    List<Template> findAll();
    
    @Insert("INSERT INTO templates (code, name, type, title_template, content_template) " +
            "VALUES (#{code}, #{name}, #{type}, #{titleTemplate}, #{contentTemplate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Template template);
}
