const express = require('express');
const router = express.Router();
const Joi = require('joi');
const Assignment = require('../models/Assignment');

// Validation schema for assignment creation
const assignmentSchema = Joi.object({
  operator_id: Joi.number().integer().required().messages({
    'number.base': 'Operator ID must be a number',
    'number.integer': 'Operator ID must be a whole number',
    'any.required': 'Operator ID is required - please specify which operator will handle this assignment'
  }),
  client_id: Joi.number().integer().required().messages({
    'number.base': 'Client ID must be a number',
    'number.integer': 'Client ID must be a whole number', 
    'any.required': 'Client ID is required - please specify which client this assignment is for'
  }),
  description: Joi.string().min(5).max(500).required().messages({
    'string.min': 'Description must be at least 5 characters long',
    'string.max': 'Description cannot exceed 500 characters',
    'string.empty': 'Description cannot be empty',
    'any.required': 'Description is required - please provide details about what work needs to be done'
  }),
  priority: Joi.string().valid('low', 'medium', 'high').default('medium').messages({
    'any.only': 'Priority must be one of: low, medium, high'
  }),
  scheduled_date: Joi.date().iso().required().messages({
    'date.base': 'Scheduled date must be a valid date',
    'date.format': 'Scheduled date must be in ISO format (e.g., 2024-12-22T09:00:00Z)',
    'any.required': 'Scheduled date is required - when should this assignment be completed?'
  })
});

// Helper function to format validation errors
const formatValidationError = (error) => {
  const details = error.details.map(detail => ({
    field: detail.path.join('.'),
    message: detail.message,
    value: detail.context?.value
  }));
  
  return {
    error: 'Validation failed',
    message: 'Please check the following fields and correct the errors:',
    details: details,
    example: {
      operator_id: 12345,
      client_id: 54321,
      description: "Fix the network connectivity issue in the main office",
      priority: "high",
      scheduled_date: "2024-12-22T09:00:00Z"
    }
  };
};

// POST /api/v1/assignments - Create new assignment
router.post('/', async (req, res) => {
  try {
    const { error } = assignmentSchema.validate(req.body);
    if (error) {
      return res.status(400).json(formatValidationError(error));
    }

    const assignment = new Assignment({
      ...req.body,
      status: 'scheduled'
    });
    await assignment.save();
    res.status(201).json(assignment);
  } catch (err) {
    console.error('Error creating assignment:', err);
    
    // Handle validation errors from mongoose
    if (err.name === 'ValidationError') {
      const details = Object.keys(err.errors).map(field => ({
        field: field,
        message: err.errors[field].message,
        value: err.errors[field].value
      }));
      
      return res.status(400).json({
        error: 'Database validation failed',
        message: 'The assignment data failed database validation:',
        details: details
      });
    }
    
    res.status(500).json({ 
      error: 'Internal server error',
      message: 'Failed to create assignment. Please try again later.',
      ...(process.env.NODE_ENV === 'development' && { details: err.message })
    });
  }
});

// GET /api/v1/assignments - List assignments
router.get('/', async (req, res) => {
  try {
    const assignments = await Assignment.find().populate('operator_id');
    res.json({
      count: assignments.length,
      assignments: assignments
    });
  } catch (err) {
    console.error('Error fetching assignments:', err);
    res.status(500).json({ 
      error: 'Internal server error',
      message: 'Failed to retrieve assignments. Please try again later.',
      ...(process.env.NODE_ENV === 'development' && { details: err.message })
    });
  }
});

// GET /api/v1/assignments/:id - Get assignment by ID
router.get('/:id', async (req, res) => {
  try {
    const assignment = await Assignment.findById(req.params.id).populate('operator_id');
    if (!assignment) {
      return res.status(404).json({ 
        error: 'Assignment not found',
        message: `No assignment found with ID: ${req.params.id}`,
        suggestion: 'Please verify the assignment ID and try again'
      });
    }
    res.json(assignment);
  } catch (err) {
    if (err.name === 'CastError') {
      return res.status(400).json({
        error: 'Invalid assignment ID',
        message: 'The provided assignment ID format is invalid',
        suggestion: 'Assignment ID should be a valid MongoDB ObjectId'
      });
    }
    
    console.error('Error fetching assignment:', err);
    res.status(500).json({ 
      error: 'Internal server error',
      message: 'Failed to retrieve assignment. Please try again later.',
      ...(process.env.NODE_ENV === 'development' && { details: err.message })
    });
  }
});

module.exports = router;