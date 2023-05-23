package com.springkotlin.springbootkotlin.service

import com.springkotlin.springbootkotlin.data.Task
import com.springkotlin.springbootkotlin.data.model.TaskCreateRequest
import com.springkotlin.springbootkotlin.data.model.TaskDto
import com.springkotlin.springbootkotlin.data.model.TaskUpdateRequest
import com.springkotlin.springbootkotlin.data.repository.TaskRepository
import com.springkotlin.springbootkotlin.exception.BadRequestException
import com.springkotlin.springbootkotlin.exception.TaskNotFoundException
import org.springframework.stereotype.Service
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Field
import java.util.stream.Collectors
import kotlin.reflect.full.memberProperties

@Service
class TaskService(private val repository: TaskRepository) {

    private fun convertEntityToDto(task: Task): TaskDto {
        return TaskDto(
            task.id,
            task.description,
            task.isReminderSet,
            task.isTaskOpen,
            task.createdOn,
            task.priority
        )
    }

    private fun assignValuesToEntity(task: Task, taskRequest: TaskCreateRequest) {
        task.description = taskRequest.description
        task.isReminderSet = taskRequest.isReminderSet
        task.isTaskOpen = taskRequest.isTaskOpen
        task.createdOn = taskRequest.createdOn
        task.priority = taskRequest.priority
    }

    private fun checkForTaskId(id: Long) {
        if (!repository.existsById(id)) {
            throw TaskNotFoundException("Task with ID: $id does not exist!")
        }
    }

    fun getAllTasks(): List<TaskDto> = repository.findAll().stream()
        .map(this::convertEntityToDto)
        .collect(Collectors.toList())

    fun getAllOpenTasks(): List<TaskDto> = repository.queryAllOpenTasks().stream()
        .map(this::convertEntityToDto)
        .collect(Collectors.toList())

    fun getAllClosedTasks(): List<TaskDto> = repository.queryAllClosedTasks().stream()
        .map(this::convertEntityToDto)
        .collect(Collectors.toList())


    fun getTaskById(id: Long): TaskDto {
        checkForTaskId(id)
        val task: Task = repository.findTaskById(id)
        return convertEntityToDto(task)
    }

    fun createTask(createRequest: TaskCreateRequest): TaskDto {
        if (repository.doesDescriptionExist(createRequest.description)) {
            throw BadRequestException("There is already a task with description: ${createRequest.description}")
        }

        val task = Task()
        assignValuesToEntity(task, createRequest)
        val savedTask = repository.save(task)
        return convertEntityToDto(savedTask)
    }

    fun updateTask(id: Long, updateRequest: TaskUpdateRequest): TaskDto {
        checkForTaskId(id)
        val existingTask: Task = repository.findTaskById(id)

        for (prop in TaskUpdateRequest::class.memberProperties) {
            if (prop.get(updateRequest) != null) {
                val field: Field? = ReflectionUtils.findField(Task::class.java, prop.name)
                field?.let {
                    it.isAccessible = true
                    ReflectionUtils.setField(it, existingTask, prop.get(updateRequest))
                }
            }
        }

        val savedTask: Task = repository.save(existingTask)
        return convertEntityToDto(savedTask)
    }

    fun deleteTask(id: Long): String {
        checkForTaskId(id)
        repository.deleteById(id)
        return "Task with ID: $id has been deleted."
    }
    
}