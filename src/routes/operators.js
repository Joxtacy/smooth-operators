const express = require('express');
const router = express.Router();
const Joi = require('joi');
const Operator = require('../models/Operator');

// Validation schema for operator creation/update
const operatorSchema = Joi.object({
  name: Joi.string().min(1).max(100).required().messages({
    'string.empty': 'Name is required and cannot be empty',
    'string.min': 'Name must be at least 1 character long',
    'string.max': 'Name cannot exceed 100 characters',
    'any.required': 'Name is required'
  }),
  email: Joi.string().email().required().messages({
    'string.email': 'Please provide a valid email address (e.g., user@example.com)',
    'string.empty': 'Email is required',
    'any.required': 'Email is required'
  }),
  skills: Joi.array().items(Joi.string()).min(1).required().messages({
    'array.min': 'At least one skill is required',
    'array.base': 'Skills must be provided as an array of strings',
    'any.required': 'Skills array is required'
  }),
  experience_years: Joi.number().integer().min(0).max(50).messages({
    'number.base': 'Experience years must be a number',
    'number.integer': 'Experience years must be a whole number',
    'number.min': 'Experience years cannot be negative',
    'number.max': 'Experience years cannot exceed 50'
  })
});

// Validation schema for search parameters
const searchSchema = Joi.object({
  skills: Joi.string().messages({
    'string.base': 'Skills parameter must be a string (comma-separated list)'
  }),
  location: Joi.string().allow('').messages({
    'string.base': 'Location parameter must be a string'
  }),
  experience_years: Joi.number().integer().min(0).max(50).messages({
    'number.base': 'Experience years must be a number',
    'number.integer': 'Experience years must be a whole number (e.g., 5, not 5.5)',
    'number.min': 'Experience years cannot be negative',
    'number.max': 'Experience years cannot exceed 50'
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
    details: details
  };
};

// GET /api/v1/operators/search - Search operators (moved before /:id to avoid conflicts)
router.get('/search', async (req, res) => {
  try {
    const { error } = searchSchema.validate(req.query);
    if (error) {
      return res.status(400).json(formatValidationError(error));
    }

    const query = {};
    if (req.query.skills) {
      query.skills = { $in: req.query.skills.split(',').map(s => s.trim()) };
    }
    if (req.query.location) {
      query.location = new RegExp(req.query.location.trim(), 'i');
    }
    if (req.query.experience_years) {
      query.experience_years = { $gte: parseInt(req.query.experience_years) };
    }

    const operators = await Operator.find(query);
    res.json({
      count: operators.length,
      operators: operators
    });
  } catch (err) {
    console.error('Error in operator search:', err);
    res.status(500).json({ 
      error: 'Internal server error',
      message: 'An error occurred while searching for operators. Please try again later.',
      ...(process.env.NODE_ENV === 'development' && { details: err.message })
    });
  }
});

// POST /api/v1/operators - Create new operator
router.post('/', async (req, res) => {
  try {
    const { error } = operatorSchema.validate(req.body);
    if (error) {
      return res.status(400).json(formatValidationError(error));
    }

    const operator = new Operator(req.body);
    await operator.save();
    res.status(201).json(operator);
  } catch (err) {
    if (err.code === 11000) {
      // Duplicate key error (email already exists)
      return res.status(409).json({
        error: 'Conflict',
        message: 'An operator with this email address already exists',
        field: 'email'
      });
    }
    
    console.error('Error creating operator:', err);
    res.status(500).json({ 
      error: 'Internal server error',
      message: 'Failed to create operator. Please try again later.',
      ...(process.env.NODE_ENV === 'development' && { details: err.message })
    });
  }
});

// GET /api/v1/operators/:id - Get operator by ID
router.get('/:id', async (req, res) => {
  try {
    const operator = await Operator.findById(req.params.id);
    if (!operator) {
      return res.status(404).json({ 
        error: 'Operator not found',
        message: `No operator found with ID: ${req.params.id}`,
        suggestion: 'Please verify the operator ID and try again'
      });
    }
    res.json(operator);
  } catch (err) {
    if (err.name === 'CastError') {
      return res.status(400).json({
        error: 'Invalid operator ID',
        message: 'The provided operator ID format is invalid',
        suggestion: 'Operator ID should be a valid MongoDB ObjectId'
      });
    }
    
    console.error('Error fetching operator:', err);
    res.status(500).json({ 
      error: 'Internal server error',
      message: 'Failed to retrieve operator. Please try again later.',
      ...(process.env.NODE_ENV === 'development' && { details: err.message })
    });
  }
});

// PUT /api/v1/operators/:id - Update operator
router.put('/:id', async (req, res) => {
  try {
    const { error } = operatorSchema.validate(req.body);
    if (error) {
      return res.status(422).json(formatValidationError(error));
    }

    const operator = await Operator.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
    if (!operator) {
      return res.status(404).json({ 
        error: 'Operator not found',
        message: `No operator found with ID: ${req.params.id}`,
        suggestion: 'Please verify the operator ID and try again'
      });
    }
    res.json(operator);
  } catch (err) {
    if (err.name === 'CastError') {
      return res.status(400).json({
        error: 'Invalid operator ID',
        message: 'The provided operator ID format is invalid',
        suggestion: 'Operator ID should be a valid MongoDB ObjectId'
      });
    }
    
    if (err.code === 11000) {
      return res.status(409).json({
        error: 'Conflict',
        message: 'An operator with this email address already exists',
        field: 'email'
      });
    }
    
    console.error('Error updating operator:', err);
    res.status(500).json({ 
      error: 'Internal server error',
      message: 'Failed to update operator. Please try again later.',
      ...(process.env.NODE_ENV === 'development' && { details: err.message })
    });
  }
});

module.exports = router;