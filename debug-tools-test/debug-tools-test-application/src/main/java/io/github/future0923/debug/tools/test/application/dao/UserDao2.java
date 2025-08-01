/*
 * Copyright (C) 2024-2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.future0923.debug.tools.test.application.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.future0923.debug.tools.test.application.domain.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserDao2 extends BaseMapperPlus<User> {

    @Select("select * from user where name = #{name} and age = #{age}")
    List<User> selectByNameAndAge(
            @Param("name") String name,
            @Param("age") Integer age);

    default List<User> selectByName(String name) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(User::getName, name);
        return selectList(queryWrapper);
    }
    //
    @Select("select * from dp_user where name = #{name}")
    List<User> aaa(@Param("name") String name);
}
