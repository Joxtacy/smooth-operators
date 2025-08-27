from dataclasses import dataclass
from typing import List, Optional, Dict, Any
import uuid
import sqlite3
import os
from datetime import datetime

@dataclass
class Operator:
    id: str
    name: str
    email: str
    phone: Optional[str] = None
    created_at: datetime = None
    updated_at: datetime = None
    
    def __post_init__(self):
        if self.created_at is None:
            self.created_at = datetime.utcnow()
        if self.updated_at is None:
            self.updated_at = datetime.utcnow()
    
    @classmethod
    def get_connection(cls):
        """Get database connection with better error handling"""
        try:
            db_path = os.getenv('DATABASE_PATH', 'operators.db')
            conn = sqlite3.connect(db_path)
            conn.row_factory = sqlite3.Row  # Enable column access by name
            return conn
        except sqlite3.Error as e:
            raise Exception(f"Database connection failed: {e}")
    
    @classmethod
    def init_database(cls):
        """Initialize database tables if they don't exist"""
        conn = cls.get_connection()
        cursor = conn.cursor()
        
        # Create operators table if it doesn't exist
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS operators (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                email TEXT UNIQUE NOT NULL,
                phone TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )
        ''')
        
        # Create skills table if it doesn't exist (for future use)
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS skills (
                id TEXT PRIMARY KEY,
                operator_id TEXT NOT NULL,
                skill_name TEXT NOT NULL,
                skill_level TEXT DEFAULT 'beginner',
                created_at TEXT NOT NULL,
                FOREIGN KEY (operator_id) REFERENCES operators (id) ON DELETE CASCADE
            )
        ''')
        
        # Create index on email for faster lookups
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_operators_email ON operators(email)')
        
        conn.commit()
        conn.close()
    
    @classmethod
    def get_all(cls) -> List['Operator']:
        """Get all operators with better error handling"""
        try:
            conn = cls.get_connection()
            cursor = conn.cursor()
            cursor.execute('SELECT id, name, email, phone, created_at, updated_at FROM operators ORDER BY created_at DESC')
            rows = cursor.fetchall()
            conn.close()
            
            operators = []
            for row in rows:
                try:
                    operators.append(cls(
                        id=row[0],
                        name=row[1],
                        email=row[2],
                        phone=row[3],
                        created_at=datetime.fromisoformat(row[4]) if row[4] else None,
                        updated_at=datetime.fromisoformat(row[5]) if row[5] else None
                    ))
                except (ValueError, TypeError) as e:
                    # Log corrupted data but continue processing other records
                    import logging
                    logging.warning(f"Skipping corrupted operator record with ID {row[0]}: {e}")
                    continue
            
            return operators
            
        except sqlite3.Error as e:
            raise Exception(f"Failed to retrieve operators: {e}")
    
    @classmethod
    def get_by_id(cls, operator_id: str) -> Optional['Operator']:
        """Get operator by ID with validation"""
        if not operator_id:
            return None
            
        try:
            conn = cls.get_connection()
            cursor = conn.cursor()
            cursor.execute('SELECT id, name, email, phone, created_at, updated_at FROM operators WHERE id = ?', (operator_id,))
            row = cursor.fetchone()
            conn.close()
            
            if row:
                try:
                    return cls(
                        id=row[0],
                        name=row[1],
                        email=row[2],
                        phone=row[3],
                        created_at=datetime.fromisoformat(row[4]) if row[4] else None,
                        updated_at=datetime.fromisoformat(row[5]) if row[5] else None
                    )
                except (ValueError, TypeError) as e:
                    import logging
                    logging.error(f"Corrupted operator data for ID {operator_id}: {e}")
                    return None
            return None
            
        except sqlite3.Error as e:
            raise Exception(f"Failed to retrieve operator {operator_id}: {e}")
    
    @classmethod
    def get_by_email(cls, email: str) -> Optional['Operator']:
        """Get operator by email address"""
        if not email:
            return None
            
        try:
            conn = cls.get_connection()
            cursor = conn.cursor()
            cursor.execute('SELECT id, name, email, phone, created_at, updated_at FROM operators WHERE email = ? COLLATE NOCASE', (email.lower(),))
            row = cursor.fetchone()
            conn.close()
            
            if row:
                try:
                    return cls(
                        id=row[0],
                        name=row[1],
                        email=row[2],
                        phone=row[3],
                        created_at=datetime.fromisoformat(row[4]) if row[4] else None,
                        updated_at=datetime.fromisoformat(row[5]) if row[5] else None
                    )
                except (ValueError, TypeError) as e:
                    import logging
                    logging.error(f"Corrupted operator data for email {email}: {e}")
                    return None
            return None
            
        except sqlite3.Error as e:
            raise Exception(f"Failed to retrieve operator by email {email}: {e}")
    
    @classmethod
    def create(cls, data: Dict[str, Any]) -> 'Operator':
        """Create a new operator with transaction support"""
        operator_id = str(uuid.uuid4())
        now = datetime.utcnow()
        
        try:
            conn = cls.get_connection()
            cursor = conn.cursor()
            
            # Use transaction to ensure data consistency
            cursor.execute('BEGIN TRANSACTION')
            
            try:
                cursor.execute('''
                    INSERT INTO operators (id, name, email, phone, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                ''', (
                    operator_id,
                    data['name'],
                    data['email'].lower(),  # Store email in lowercase for consistency
                    data.get('phone'),
                    now.isoformat(),
                    now.isoformat()
                ))
                
                cursor.execute('COMMIT')
                
            except sqlite3.Error:
                cursor.execute('ROLLBACK')
                raise
                
            finally:
                conn.close()
            
            return cls(
                id=operator_id,
                name=data['name'],
                email=data['email'],
                phone=data.get('phone'),
                created_at=now,
                updated_at=now
            )
            
        except sqlite3.IntegrityError as e:
            if 'email' in str(e).lower():
                raise Exception(f"Email address '{data['email']}' is already in use")
            else:
                raise Exception(f"Data integrity error: {e}")
        except sqlite3.Error as e:
            raise Exception(f"Failed to create operator: {e}")
    
    def update(self, data: Dict[str, Any]) -> 'Operator':
        """Update operator with transaction support"""
        now = datetime.utcnow()
        
        # Track what fields are being updated
        updated_fields = []
        
        # Update fields that are provided
        if 'name' in data:
            self.name = data['name']
            updated_fields.append('name')
        if 'email' in data:
            self.email = data['email']
            updated_fields.append('email')
        if 'phone' in data:
            self.phone = data['phone']
            updated_fields.append('phone')
        
        self.updated_at = now
        
        try:
            conn = self.get_connection()
            cursor = conn.cursor()
            
            cursor.execute('BEGIN TRANSACTION')
            
            try:
                cursor.execute('''
                    UPDATE operators 
                    SET name = ?, email = ?, phone = ?, updated_at = ?
                    WHERE id = ?
                ''', (self.name, self.email.lower() if self.email else None, self.phone, now.isoformat(), self.id))
                
                if cursor.rowcount == 0:
                    raise Exception(f"Operator with ID {self.id} not found")
                
                cursor.execute('COMMIT')
                
            except sqlite3.Error:
                cursor.execute('ROLLBACK')
                raise
                
            finally:
                conn.close()
            
            return self
            
        except sqlite3.IntegrityError as e:
            if 'email' in str(e).lower():
                raise Exception(f"Email address '{self.email}' is already in use")
            else:
                raise Exception(f"Data integrity error: {e}")
        except sqlite3.Error as e:
            raise Exception(f"Failed to update operator: {e}")
    
    def delete(self):
        """Delete operator with cascade support"""
        try:
            conn = self.get_connection()
            cursor = conn.cursor()
            
            cursor.execute('BEGIN TRANSACTION')
            
            try:
                # Delete related skills first (if any)
                cursor.execute('DELETE FROM skills WHERE operator_id = ?', (self.id,))
                
                # Delete the operator
                cursor.execute('DELETE FROM operators WHERE id = ?', (self.id,))
                
                if cursor.rowcount == 0:
                    raise Exception(f"Operator with ID {self.id} not found")
                
                cursor.execute('COMMIT')
                
            except sqlite3.Error:
                cursor.execute('ROLLBACK')
                raise
                
            finally:
                conn.close()
                
        except sqlite3.Error as e:
            raise Exception(f"Failed to delete operator: {e}")
    
    def get_skills(self):
        """Get skills for this operator"""
        try:
            conn = self.get_connection()
            cursor = conn.cursor()
            cursor.execute('''
                SELECT id, skill_name, skill_level, created_at 
                FROM skills 
                WHERE operator_id = ? 
                ORDER BY created_at DESC
            ''', (self.id,))
            rows = cursor.fetchall()
            conn.close()
            
            skills = []
            for row in rows:
                try:
                    skills.append({
                        'id': row[0],
                        'skill_name': row[1],
                        'skill_level': row[2],
                        'created_at': row[3]
                    })
                except (ValueError, TypeError) as e:
                    import logging
                    logging.warning(f"Skipping corrupted skill record: {e}")
                    continue
            
            return skills
            
        except sqlite3.Error as e:
            import logging
            logging.error(f"Failed to retrieve skills for operator {self.id}: {e}")
            return []  # Return empty list instead of raising exception
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary with proper serialization"""
        return {
            'id': self.id,
            'name': self.name,
            'email': self.email,
            'phone': self.phone,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'updated_at': self.updated_at.isoformat() if self.updated_at else None
        }

# Initialize database when module is imported
try:
    Operator.init_database()
except Exception as e:
    import logging
    logging.error(f"Failed to initialize database: {e}")