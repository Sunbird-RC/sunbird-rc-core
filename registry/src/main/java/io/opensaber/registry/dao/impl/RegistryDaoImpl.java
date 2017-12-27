package io.opensaber.registry.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.datastax.driver.core.querybuilder.Assignment;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.datastax.driver.core.querybuilder.Update;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import io.opensaber.registry.config.CassandraConfiguration;
import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.model.Teacher;
import io.opensaber.registry.model.dto.EntityDto;

/**
 * 
 * @author jyotsna
 *
 */
@Service
public class RegistryDaoImpl implements RegistryDao{
	
	@Autowired
	CassandraConfiguration cassandraConfig;
	
	@Autowired
	ObjectMapper objectMapper;
	
	@Override
	public List getEntityList(){
		List<Teacher> teacherList = new ArrayList<Teacher>();
		try{
			String query1 = "select userid from user_job_profile where jobname in ('professor') allow filtering";
			List<String> userIdList = cassandraConfig.cassandraTemplate().select(query1,String.class);
			Select selectQuery = QueryBuilder.select().column("id")
					.column("dob").column("email").column("avatar")
					.column("emailverified").column("firstname").column("lastname")
					.column("gender").column("language").column("grade").from("user").allowFiltering();
			Where selectWhere = selectQuery.where();
			Clause whereClause = QueryBuilder.in("id", userIdList);
			selectWhere.and(whereClause);
			teacherList = cassandraConfig.cassandraTemplate().select(selectQuery,Teacher.class);
			return teacherList;
		}catch(Exception e){
			e.printStackTrace();
		}
		return teacherList;
	}
	
	@Override
	public boolean addEntity(Object entity){
		try{
			Map<String,Object> insertValues = objectMapper.convertValue(entity,  new TypeReference<HashMap<String, Object>>() {});
			Insert insertQuery = QueryBuilder.insertInto("user").values(Lists.newArrayList(insertValues.keySet()), Lists.newArrayList(insertValues.values()));
			cassandraConfig.cassandraTemplate().execute(insertQuery);
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}
	
	@Override
	public boolean updateEntity(Object entity){
		try{
			Map<String,Object> insertValues = objectMapper.convertValue(entity,  new TypeReference<HashMap<String, Object>>() {});
			Update updateQuery = QueryBuilder.update("user");
			Clause whereClause = QueryBuilder.eq("id", (String)insertValues.get("id"));
			Update.Where whereQuery = updateQuery.where();
			whereQuery.and(whereClause);
			insertValues.remove("id");
			for(Map.Entry<String, Object> entry : insertValues.entrySet()){
				Assignment assignment = QueryBuilder.set(entry.getKey(), entry.getValue());
				updateQuery.with(assignment);
			}
			cassandraConfig.cassandraTemplate().execute(updateQuery);
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}
	
	@Override
	public Object getEntityById(EntityDto entityDto){
		try{
			Select selectQuery = QueryBuilder.select().column("id")
					.column("dob").column("email").column("avatar")
					.column("emailverified").column("firstname").column("lastname")
					.column("gender").column("language").column("grade").from("user").allowFiltering();
			Where selectWhere = selectQuery.where();
			Clause whereClause = QueryBuilder.eq("id", entityDto.getId());
			selectWhere.and(whereClause);
			return cassandraConfig.cassandraTemplate().selectOne(selectQuery,Teacher.class);
		}catch(Exception e){
			e.printStackTrace();
		}
		return new Teacher();
	}
	
	@Override
	public boolean deleteEntity(EntityDto entityDto){
		try{
			Delete deleteQuery = QueryBuilder.delete().all().from("user");
			Delete.Where whereQuery = deleteQuery.where();
			Clause whereClause = QueryBuilder.eq("id", entityDto.getId());
			whereQuery.and(whereClause);
			cassandraConfig.cassandraTemplate().execute(deleteQuery);
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}		
		return false;
	}

}
