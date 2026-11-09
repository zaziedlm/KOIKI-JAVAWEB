import pg from 'pg';
import {config,purpose,rpConfig} from './config.ts';
import {csrfFor,equal,hash,opaque,open,seal} from './crypto.ts';
let shared:pg.Pool|undefined;
export function pool(){return shared??=new pg.Pool({host:'db',port:5432,database:'koiki_bff',user:'koiki_bff',
  password:config().databasePassword,max:4,min:1,connectionTimeoutMillis:3000,statement_timeout:5000,lock_timeout:5000});}
const validId=(id:string|undefined)=>Boolean(id && /^[A-Za-z0-9_-]{43}$/.test(id));
export function sessionValid(row:{idle_expires_at:Date;absolute_expires_at:Date;access_token_expires_at:Date},now=Date.now()) {
  return [row.idle_expires_at,row.absolute_expires_at,row.access_token_expires_at].every(d=>d.getTime()>now);
}
async function finitePurge() {
  await pool().query("DELETE FROM bff_auth_transaction WHERE ctid IN (SELECT ctid FROM bff_auth_transaction WHERE expires_at<=current_timestamp ORDER BY expires_at LIMIT 100)");
  await pool().query("DELETE FROM bff_session WHERE ctid IN (SELECT ctid FROM bff_session WHERE absolute_expires_at<=current_timestamp OR idle_expires_at<=current_timestamp OR access_token_expires_at<=current_timestamp ORDER BY absolute_expires_at LIMIT 100)");
}
export async function saveTransaction(binding:string,expected:{state:string;verifier?:string;nonce?:string;kind?:string},rp='a') {
  rpConfig(rp);await finitePurge();const digest=hash(expected.state),aad='auth:'+rp+':'+digest,key=config(),cipher=seal(key.key,expected,aad);
  await pool().query(`INSERT INTO bff_auth_transaction(client_id,state_hash,browser_binding_hash,protected_payload,encryption_algorithm,encryption_key_id,encryption_iv,encryption_tag,expires_at)
    VALUES($7,$1,$2,$3,'aes-256-gcm',$4,$5,$6,current_timestamp+interval '5 minutes')`,
    [digest,hash(binding),cipher.payload,key.keyId,cipher.iv,cipher.tag,rp]);
}
export async function consumeTransaction(binding:string,state:string,rp='a',kind='auth') {
  rpConfig(rp);if(!validId(binding) || !validId(state))return null;
  const db=await pool().connect();
  try {
    await db.query('BEGIN');
    const result=await db.query("SELECT * FROM bff_auth_transaction WHERE client_id=$2 AND state_hash=$1 FOR UPDATE",[hash(state),rp]);
    const row=result.rows[0];
    if(!row || row.consumed_at || new Date(row.expires_at).getTime()<=Date.now() || !equal(row.browser_binding_hash,hash(binding))) {
      await db.query('ROLLBACK');return null;
    }
    if(row.encryption_key_id!==config().keyId)throw new Error('Invalid transaction key');
    const value=open(config().key,{payload:row.protected_payload,iv:row.encryption_iv,tag:row.encryption_tag},'auth:'+rp+':'+hash(state));
    if((value.kind??'auth')!==kind){await db.query('ROLLBACK');return null;}
    await db.query("UPDATE bff_auth_transaction SET consumed_at=current_timestamp WHERE client_id=$2 AND state_hash=$1",[hash(state),rp]);
    await db.query('COMMIT');return value;
  } catch(error){await db.query('ROLLBACK');throw error;}finally{db.release();}
}
export async function createSession(tokens:{access_token:string;refresh_token?:string;id_token?:string;expires_in?:number},issuer:string,subject:string,rp='a') {
  rpConfig(rp);if(!tokens.access_token || !tokens.id_token || !tokens.refresh_token || !tokens.expires_in || tokens.expires_in<10)throw new Error('Invalid tokens');
  const id=opaque(),digest=hash(id),key=config(),cipher=seal(key.key,tokens,'session:'+rp+':'+digest),csrf=csrfFor(key.key,purpose('session',rp),id);
  await pool().query(`INSERT INTO bff_session(client_id,session_id_hash,issuer,subject,csrf_hash,protected_tokens,encryption_algorithm,encryption_key_id,encryption_iv,encryption_tag,access_token_expires_at,idle_expires_at,absolute_expires_at)
    VALUES($10,$1,$2,$3,$4,$5,'aes-256-gcm',$6,$7,$8,$9,current_timestamp+interval '15 minutes',current_timestamp+interval '60 minutes')`,
    [digest,issuer,subject,hash(csrf),cipher.payload,key.keyId,cipher.iv,cipher.tag,new Date(Date.now()+(tokens.expires_in-5)*1000),rp]);
  return id;
}
export async function session(id:string|undefined,rp='a') {
  rpConfig(rp);if(!validId(id))return null;
  const db=await pool().connect();
  try {
    await db.query('BEGIN');
    const result=await db.query("SELECT * FROM bff_session WHERE client_id=$2 AND session_id_hash=$1 FOR UPDATE",[hash(id!),rp]);
    const row=result.rows[0];if(!row){await db.query('ROLLBACK');return null;}
    if(!sessionValid(row) || row.encryption_key_id!==config().keyId) {
      await db.query("DELETE FROM bff_session WHERE client_id=$2 AND session_id_hash=$1",[hash(id!),rp]);
      await db.query('COMMIT');return null;
    }
    const csrf=csrfFor(config().key,purpose('session',rp),id!);
    if(!equal(hash(csrf),row.csrf_hash))throw new Error('Invalid Session binding');
    const tokens=open(config().key,{payload:row.protected_tokens,iv:row.encryption_iv,tag:row.encryption_tag},'session:'+rp+':'+hash(id!));
    await db.query("UPDATE bff_session SET last_seen_at=current_timestamp,idle_expires_at=LEAST(absolute_expires_at,current_timestamp+interval '15 minutes'),version=version+1 WHERE client_id=$2 AND session_id_hash=$1",[hash(id!),rp]);
    await db.query('COMMIT');return {subject:row.subject,csrf,tokens};
  } catch(error){await db.query('ROLLBACK');throw error;}finally{db.release();}
}
export async function removeSession(id:string,rp='a') {
  rpConfig(rp);if(validId(id))await pool().query("DELETE FROM bff_session WHERE client_id=$2 AND session_id_hash=$1",[hash(id),rp]);
}
