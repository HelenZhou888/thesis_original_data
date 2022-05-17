package models;

import java.util.ArrayList;
import java.util.List;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloMultiCriterionExpr;
import ilog.concert.IloNumExpr;
import ilog.cp.IloCP;
import objectives.EdgeServer;
import objectives.User;

public class LiuICWSModel {

	private double mAllUsers;
	private double mCost;
	private double mbenefitefficiency;
	private double mAllBenefits;
	private double mSquare;
	private double mAllBenefit_square;

	private int mServersNumber;
	private int mBudget;
	private double mFairness_index;
	private int[][] mAdjacencyMatrix;
	private int[][] mUserCovered; 
	private int[][] mUserBenefits;
	private List<User> mUsers;// All Users
	
	//2021
	//private double mfairnessindex;
	private double mfairnessdegree; //make the original value = 0
	private double mfairness_efficiency;
    private double mfairness_degree;
	//no need to add another variable for caching cost, we can use the 
	
	private List<Integer> mValidUserList; //Covered Users by selected servers
	
	private List<Integer> mSelectedServerList;
	private List<Integer> mBenefitList;
	//private List<Integer> mUserList;
	
	public LiuICWSModel (int serversNumber, double fairness_index, int[][] adjacencyMatrix, int[][] userCovered, int[][] userBenefits, List<User> users, 
			List<EdgeServer> servers) {
		mServersNumber = serversNumber;
		mFairness_index = fairness_index;
		mAdjacencyMatrix = adjacencyMatrix;
		
		//������benefit
	//	mUserCovered = userCovered;
		mUsers = users;
		
		//������benefit
		mUserBenefits = userCovered;
		mValidUserList = new ArrayList<>();
		mSelectedServerList = new ArrayList<>();
		
		//mUserList = new ArrayList<>();
		mBenefitList = new ArrayList<>();	
	}
	
	public void runLiuICWS() {
		
		try {
			//new cplex objective
			IloCP cp = new IloCP();
			
			//r ��caching���� 0/1
			IloIntVar[] r = cp.intVarArray(mServersNumber, 0, 1);
			
			//���Ա��� ��ɶ�ð���
			IloNumExpr[] eExpr = new IloNumExpr[3];
			
			//�ڶ������Ա��� ȫ���û�benefit�ĺ�
			IloLinearNumExpr rExpr = cp.linearNumExpr();
			//�ڶ������Ա��� ȫ���û�benefit�ĺ�
			IloNumExpr dExpr = cp.linearNumExpr();
			
			//���������Ա��� ȫ���û�benefit��ƽ���ĺ�
			IloNumExpr sExpr = cp.linearNumExpr();
			
			//
			IloNumExpr ddExpr = cp.linearNumExpr();
			
			//
			IloNumExpr jainExpr = cp.linearNumExpr();
			
			//ȫ����cover���û���������������user������������
			IloNumExpr[] maxBenifitsExprs = new IloNumExpr[mUsers.size()];
			
			//��������һ��һά���飬�洢ÿ��benefit��ƽ��
			IloNumExpr[] maxSquareBenifitsExprs = new IloNumExpr[mUsers.size()];
			
			//�û���server֮��cover��ϵ�Ķ�ά����
			IloNumExpr[][] userBenefitsExprs = new IloNumExpr[mUsers.size()][mServersNumber];

			//�����Ǽ����û�cover�Ķ�ά���� ��caching��һά����
			for (int i = 0; i < mServersNumber; i++) {
				rExpr.addTerm(1, r[i]);
				for (int j = 0; j < mUsers.size(); j++) {
					userBenefitsExprs[j][i] = cp.prod(mUserBenefits[i][j], r[i]);
				}
			}

			//�������ȫ����benefit һά����
			for (int j = 0; j < mUsers.size(); j++) {
				maxBenifitsExprs[j] = cp.max(userBenefitsExprs[j]);
			}

			//���������Ա��� ȫ��benefit���ܺ�
			dExpr = cp.sum(maxBenifitsExprs);
			
			ddExpr = cp.square(dExpr);
		
			
			//���� maxSquare����洢ÿ���û�benefit��ƽ��
			for (int j = 0; j < mUsers.size(); j++) {
				maxSquareBenifitsExprs[j] = cp.prod(maxBenifitsExprs[j],maxBenifitsExprs[j]);
			}
			
			//������ÿ��benefit��ƽ���ĺ� ��ĸ
			sExpr = cp.sum(maxSquareBenifitsExprs);
			sExpr = cp.prod(sExpr, mUsers.size());
			
			jainExpr = cp.quot(ddExpr, sExpr);
			
			eExpr[0] = rExpr;
			eExpr[1] = cp.negative(dExpr);
			eExpr[2] = cp.negative(sExpr);

			IloMultiCriterionExpr moExpr = cp.staticLex(eExpr);

			cp.add(cp.minimize(moExpr));
			//cp.add(cp.maximize(dExpr));

		//	IloConstraint u = cp.ge(dExpr,(mUsers.size()*2));//�û�benefit��ֵ������cover�û�������ֵ ���� ���ڵ�������ȫ���û�size
			IloConstraint u = cp.ge(dExpr,mUsers.size());//�û�benefit��ֵ������cover�û�������ֵ ���� ���ڵ�������ȫ���û�size
		//	IloConstraint u = cp.ge(jainExpr, 0.75);
		//	for (int i = 0; i < mUsers.size(); i++) {
		//		u = cp.and(u, cp.ge(maxBenifitsExprs[i], 1)); //ÿ���û���benefit������Ϊ1
		//	}
		//	IloConstraint u = cp.ge(mfairnessdegree,0.95);
			cp.add(u);
			cp.setOut(null);

			if (cp.solve()) {	
				mAllUsers = cp.getObjValue();
				mCost = cp.getObjValues()[0];
				mAllBenefits = -cp.getObjValues()[1];
				mSquare = -cp.getObjValues()[2];
				mfairnessdegree = (mAllBenefits * mAllBenefits) / mSquare;
				mfairness_efficiency = mfairnessdegree / mCost;
				mAllUsers = mAllBenefits;
				
				/*mCost = cp.getIncumbentValue(rExpr);
				mAllBenefits = cp.getObjValue();		
				mfairnessdegree = cp.getIncumbentValue(jainExpr);
				mfairness_efficiency = mfairnessdegree / mCost;
				mAllUsers = mAllBenefits;*/
				
			} else {
				System.out.println(" No solution found ");
			}
			cp.end();
		} catch (IloException e) {
			System.err.println("Concert exception caught: " + e);
		}
	}
	
	//���ܼ���������㰡
//	private double calculateBenefits(IloNumExpr a, IloNumExpr b) {
//		double c;
//		c = a/b;
//		return c;
//	}
	
	public double getAllUsers() {
		return mAllUsers;
	}
	
	public double getcost() {
		return mCost;
	}
	
	public double getfairness_degree() {
		return mfairnessdegree;
	}
	
	public double getfairness_efficiency() {
		return mfairness_efficiency;
	}
	
	public double getallbenefits() {
		return mAllBenefits = mAllBenefits/100;
	}
	
	public double getbenefit_efficiency() {
		return mbenefitefficiency = mAllBenefits/mCost;
	}
	
}

/*
 package models;

import java.util.ArrayList;
import java.util.List;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloMultiCriterionExpr;
import ilog.concert.IloNumExpr;
import ilog.cp.IloCP;
import objectives.EdgeServer;
import objectives.User;

public class LiuICWSModel {

	private double mAllUsers;
	private double mCost;
	private double mAllBenefits;
	private double mAllBenefit_square;

	private int mServersNumber;
	private int mBudget;
	private double mFairness_index;
	private int[][] mAdjacencyMatrix;
	private int[][] mUserCovered; 
	private int[][] mUserBenefits;
	private List<User> mUsers;// All Users
	
	//2021
	//private double mfairnessindex;
	private double mfairnessdegree; //make the original value = 0
	private double mfairness_efficiency;
    private double mfairness_degree;
	//no need to add another variable for caching cost, we can use the 
	
	private List<Integer> mValidUserList; //Covered Users by selected servers
	
	private List<Integer> mSelectedServerList;
	private List<Integer> mBenefitList;
	//private List<Integer> mUserList;
	
	public LiuICWSModel (int serversNumber, double fairness_index, int[][] adjacencyMatrix, int[][] userCovered, int[][] userBenefits, List<User> users, 
			List<EdgeServer> servers) {
		mServersNumber = serversNumber;
		mFairness_index = fairness_index;
		mAdjacencyMatrix = adjacencyMatrix;
		
		//������benefit
	//	mUserCovered = userCovered;
		mUsers = users;
		
		//������benefit
		mUserBenefits = userCovered;
		mValidUserList = new ArrayList<>();
		mSelectedServerList = new ArrayList<>();
		
		//mUserList = new ArrayList<>();
		mBenefitList = new ArrayList<>();	
	}
	
	public void runLiuICWS() {
		
		try {
			//new cplex objective
			IloCP cp = new IloCP();
			
			//r ��caching���� 0/1
			IloIntVar[] r = cp.intVarArray(mServersNumber, 0, 1);
			
			//���Ա��� ��ɶ�ð���
			IloLinearNumExpr rExpr = cp.linearNumExpr();
			
			//�ڶ������Ա��� ȫ���û�benefit�ĺ�
			IloNumExpr dExpr = cp.linearNumExpr();
			
			//���������Ա��� ȫ���û�benefit��ƽ���ĺ�
			IloNumExpr sExpr = cp.linearNumExpr();
			
			//�����洢����֮һ��benefit  �����ܴ���ȫ��cover���û������� ��Ϊcover��benefit��2
			IloNumExpr uExpr = cp.linearNumExpr();
			
			//���յ��Ż�
			IloNumExpr oExpr = cp.linearNumExpr();
			
			//ȫ����cover���û���������������user������������
			IloNumExpr[] maxBenifitsExprs = new IloNumExpr[mUsers.size()];
			
			//��������һ��һά���飬�洢ÿ��benefit��ƽ��
			IloNumExpr[] maxSquareBenifitsExprs = new IloNumExpr[mUsers.size()];
			
			//�û���server֮��cover��ϵ�Ķ�ά����
			IloNumExpr[][] userBenefitsExprs = new IloNumExpr[mUsers.size()][mServersNumber];

			//�����Ǽ����û�cover�Ķ�ά���� ��caching��һά����
			for (int i = 0; i < mServersNumber; i++) {
				rExpr.addTerm(1, r[i]);
				for (int j = 0; j < mUsers.size(); j++) {
					userBenefitsExprs[j][i] = cp.prod(mUserBenefits[i][j], r[i]);
				}
			}

			//�������ȫ����benefit һά����
			for (int j = 0; j < mUsers.size(); j++) {
				maxBenifitsExprs[j] = cp.max(userBenefitsExprs[j]);
			}

			//���������Ա��� ȫ��benefit���ܺ�
			dExpr = cp.sum(maxBenifitsExprs);
			
			//��������û�����  ����֮һ��benefit
			uExpr = cp.prod(dExpr, 0.5);
			
			//ȫ��benefit�ĺ͵�ƽ�� ����
			dExpr = cp.prod(dExpr, dExpr);

			//���� maxSquare����洢ÿ���û�benefit��ƽ��
			for (int j = 0; j < mUsers.size(); j++) {
				maxSquareBenifitsExprs[j] = cp.prod(maxBenifitsExprs[j],maxBenifitsExprs[j]);
			}
			
			//������ÿ��benefit��ƽ���ĺ� ��ĸ
			sExpr = cp.sum(maxSquareBenifitsExprs);
			
            //���ڰ�benefitƽ���ĺͳ���ȫ���û������� ��Ϊfairness�ķ�ĸ
			//sExpr = uExpr/sExpr;
			//oExpr = calculateBenefits(dExpr, sExpr);
			//oExpr = cp.div(dExpr, sExpr);
			cp.add(cp.maximize(dExpr));
			
			IloConstraint u = cp.ge(mbudget,rExpr);//rExpr С�ڵ���budget ˵��ѡ��caching��server����С�ڵ���budget
			cp.add(u);

			cp.setOut(null);

			if (cp.solve()) {	
				mAllUsers = cp.getObjValue();
			} else {
				System.out.println(" No solution found ");
			}
			cp.end();
		} catch (IloException e) {
			System.err.println("Concert exception caught: " + e);
		}
	}
	
	//���ܼ���������㰡
//	private double calculateBenefits(IloNumExpr a, IloNumExpr b) {
//		double c;
//		c = a/b;
//		return c;
//	}
	
	
	public double getAllUsers() {
		return mAllUsers;
	}
	
	public double getcost() {
		return mCost;
	}
	
	public double getfairness_degree() {
		return mfairnessdegree;
	}
	
	public double getfairness_efficiency() {
		return mfairness_efficiency;
	}
	
}
*/
